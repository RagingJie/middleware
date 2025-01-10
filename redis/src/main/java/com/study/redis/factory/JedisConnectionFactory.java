package com.study.redis.factory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

// Jedis连接redis是线程不安全的，所以需要一个工厂类来获取Jedis实例
public class JedisConnectionFactory {

    private static final JedisPool jedisPool;

    static {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        // 最大等待时间
        jedisPoolConfig.setMaxWaitMillis(2000);
        // 最大连接数
        jedisPoolConfig.setMaxTotal(8);
        // 最小空闲连接数
        jedisPoolConfig.setMinIdle(0);
        // 最大空闲连接数
        jedisPoolConfig.setMaxIdle(8);
        jedisPool = new JedisPool(jedisPoolConfig,"127.0.0.1",6379,2000,"123456");
    }

    public static Jedis getJedis() {
        return jedisPool.getResource();
    }
}
