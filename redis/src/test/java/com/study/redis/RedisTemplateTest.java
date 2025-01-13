package com.study.redis;

import com.study.redis.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@Slf4j
@SpringBootTest
public class RedisTemplateTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    public void test() {
        ValueOperations strOperations = redisTemplate.opsForValue();
        String name1 = "name";
        strOperations.set(name1, "haha");
        String name = strOperations.get("name").toString();
        System.out.println("redis获取key结果 => " + name);
    }


    @Test
    public void test2() {
        redisTemplate.opsForValue().set("user:1", new User("张山", 18, "男"));

        User result = (User) redisTemplate.opsForValue().get("user:1");
        log.info("redis获取key结果 => {}", result);
    }
}
