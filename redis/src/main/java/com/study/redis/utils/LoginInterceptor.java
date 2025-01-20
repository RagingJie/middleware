package com.study.redis.utils;

import com.study.redis.dto.UserDTO;
import com.study.redis.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        log.info("拦截请求：{}", requestURI);

        // 1、获取session
        HttpSession session = request.getSession();
        // 2、获取用户信息
        Object user = session.getAttribute("user");
        // 3、判断用户信息是否为空
        if (user == null) {
            // 4、如果不为空，则放行
            response.setStatus(401);
            return false;
        }
        // 5、将用户信息存入ThreadLocal
        UserHolder.saveUser((UserDTO) user);
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
