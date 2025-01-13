package com.study.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.redis.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@Slf4j
public class RedisStringTemplateTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testValue() throws JsonProcessingException {
        User user = new User("阿虎", 18, "男");
        String userOne = objectMapper.writeValueAsString(user);
        stringRedisTemplate.opsForValue().set("user:1001", userOne, 10, TimeUnit.SECONDS);

        String result = stringRedisTemplate.opsForValue().get("user:1001");
        User userTwo = objectMapper.readValue(result, User.class);
        log.info("反序列化的结果 => {}", userTwo);
    }

    @Test
    public void testHash() {
        stringRedisTemplate.opsForHash().put("user:1002", "name", "张三");
        stringRedisTemplate.opsForHash().put("user:1002", "age", "19");
        stringRedisTemplate.opsForHash().put("user:1002", "phone", "18988888888");


        Object name = stringRedisTemplate.opsForHash().get("user:1002", "name");
        log.info("name => {}", name);

        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries("user:1002");
        log.info("entries => {}", entries);
    }
}
