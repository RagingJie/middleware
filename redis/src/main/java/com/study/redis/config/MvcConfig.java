package com.study.redis.config;

import com.study.redis.utils.LoginInterceptor;
import com.study.redis.utils.RefreshTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;
    @Autowired
    private RefreshTokenInterceptor refreshTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/code",
                        "/user/login",
                        "/blog/**",
                        "/shop-type/**",
                        "/upload/**",
                        "/voucher/**",
                        "/shop/**"
                )
                .order(1);

        registry.addInterceptor(refreshTokenInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/user/code")
                .excludePathPatterns("/user/login")
                .excludePathPatterns("/shop/**")
                .excludePathPatterns("/voucher/seckill")
                .order(0);
    }
}
