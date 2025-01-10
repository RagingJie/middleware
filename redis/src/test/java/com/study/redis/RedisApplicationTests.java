package com.study.redis;

import com.study.redis.factory.JedisConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import redis.clients.jedis.Jedis;

import java.util.Map;

@SpringBootTest
class RedisApplicationTests {
    private Jedis jedis;

    @Test
    void contextLoads() {
    }

    @BeforeEach
    public void contectRedis() {
        // 1、建立链接
//        jedis = new Jedis("127.0.0.1", 6379, 2000);
        jedis = JedisConnectionFactory.getJedis();
        // 2、输入密码
        jedis.auth("123456");
        // 选择数据库
        jedis.select(14);
    }

    @Test
    public void testString() {
        String result = jedis.set("tom", "是个好狗");
        System.out.println("redis设置key结果 => " + result);

        String tom = jedis.get("tom");
        System.out.println("redis获取key结果 => " + tom);
    }


    @Test
    public void testHash() {
        Long result1 = jedis.hset("user:1", "name", "tom");
        Long result2 = jedis.hset("user:1", "age", "12");
        System.out.println("redis设置key结果 => " + result1 + ", " + result2);

        Map<String, String> map = jedis.hgetAll("user:1");
        System.out.println("redis获取key结果 => " + map);

    }

    @AfterEach
    public void closeRedis() {
        if (jedis != null) {
            jedis.close();
        }
    }

}
