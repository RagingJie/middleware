package com.study.redis.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.redis.dto.LoginFormDTO;
import com.study.redis.dto.Result;
import com.study.redis.dto.UserDTO;
import com.study.redis.entity.User;
import com.study.redis.mapper.UserMapper;
import com.study.redis.service.IUserService;
import com.study.redis.utils.RedisConstants;
import com.study.redis.utils.RegexPatterns;
import com.study.redis.utils.RegexUtils;
import com.study.redis.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 猫哥实现的 wangwangwang
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private HttpServletRequest request;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1、手机号是否为空
        if (phone == null) {
            return Result.fail("手机号为空");
        }
        // 2、手机号格式是否正确
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        // 3、生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 4、保存验证码到session
//        session.setAttribute("code", code);
        // 4、将验证码保存到redis中
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, code, RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
        // 5、发送短信
        log.debug("发送短信验证码成功，验证码 => {}", code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1、校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        // 2、校验验证码
//        String code = session.getAttribute("code").toString();
//        if (loginForm.getCode() == null || !loginForm.getCode().equals(code)) {
//            return Result.fail("验证码错误");
//        }
        String code = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
        if (code == null) return Result.fail("验证码已过期");
        if (!loginForm.getCode().equals(code)) {
            return Result.fail("验证码错误");
        }
        // 3、根据手机号查询用户
        User user = query().eq("phone", phone).one();
        if (user == null) {
            user = createUserWithPhone(phone);
        }

        // 4、保存用户信息到session
        // session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
        // 4、存储到redis中
        // token
        String token = UUID.randomUUID().toString(true);
        // 隐藏用户敏感数据userDTO
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(), CopyOptions.create().setIgnoreNullValue(false).setFieldValueEditor((filedName, filedValue) -> filedValue.toString()));
        // 存储到redis中
        String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

        // 5、返回结果
        return Result.ok(token);
    }

    @Override
    public Result logout() {
        String token = request.getHeader("authorization");
        if (token == null) {
            return Result.fail("用户未登录");
        }
        stringRedisTemplate.delete(RedisConstants.LOGIN_USER_KEY + token);
        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(6));
        save(user);
        return user;
    }
}
