package com.study.redis.utils;

import cn.hutool.core.bean.BeanUtil;
import com.study.redis.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RefreshTokenInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        log.info("拦截请求：{}", requestURI);

        // 获取token
        String token = request.getHeader("authorization");

        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY + token);
        if (userMap.isEmpty()){
            response.setStatus(401);
            return false;
        }

        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        if (userDTO == null) {
            // 4、如果不为空，则放行
            response.setStatus(401);
            return false;
        }
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        // 1、获取session
//        HttpSession session = request.getSession();
        // 2、获取用户信息
//        Object user = session.getAttribute("user");
        // 3、判断用户信息是否为空
//        if (user == null) {
//            // 4、如果不为空，则放行
//            response.setStatus(401);
//            return false;
//        }
        // 5、将用户信息存入ThreadLocal
        UserHolder.saveUser(userDTO);
        // 6、放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清除ThreadLocal
        log.info("清理线程局部变量");
        UserHolder.removeUser();
        UserDTO user = UserHolder.getUser();
        log.info("清理线程局部变量 -- 当前线程用户信息：{}", user);
    }
}
