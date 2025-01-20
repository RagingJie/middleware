package com.study.redis.utils;

import cn.hutool.core.bean.BeanUtil;
import com.study.redis.dto.UserDTO;
import com.study.redis.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        log.info("拦截请求：{}", requestURI);
        UserDTO user = UserHolder.getUser();
        // 判断是否登录
        if (user == null) {
            response.setStatus(401);
            return false;
        }
        // 放行
        return true;
    }

}
