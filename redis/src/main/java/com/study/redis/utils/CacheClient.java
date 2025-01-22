package com.study.redis.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
public class CacheClient {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 设置缓存
     *
     * @param key        缓存键
     * @param value      缓存值
     * @param expireTime 过期时间
     * @param timeUnit   时间单位
     */
    public void set(String key, Object value, Long expireTime, TimeUnit timeUnit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), expireTime, timeUnit);
    }

    /**
     * 设置逻辑过期
     *
     * @param key        缓存键
     * @param value      缓存值
     * @param expireTime 过期时间
     * @param timeUnit   时间单位
     */
    public void setWithLogicalExpire(String key, Object value, Long expireTime, TimeUnit timeUnit) {
        // 设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(expireTime)));
        // 写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 缓存穿透
     *
     * @param keyPrefix  缓存前缀
     * @param id         id
     * @param type       类型
     * @param dbFallBack 回调函数
     * @param time       时间
     * @param unit       时间单位
     * @param <R>        返回值类型
     * @param <ID>       id类型
     * @return 返回值
     */
    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallBack, Long time, TimeUnit unit) {
        // 拼接缓存key
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }
        // 空字符串，返回null
        if (json != null) {
            return null;
        }
        // 不存在，根据id查询数据库
        R r = dbFallBack.apply(id);
        // 不存在，返回null
        if (r == null) {
            // 将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "", time, unit);
            return null;
        }
        this.set(key, r, time, unit);
        return r;
    }

    /**
     * 逻辑过期解决缓存击穿
     *
     * @param keyPrefix  缓存前缀
     * @param id         id
     * @param type       类型
     * @param dbFallBack 回调函数
     * @param time       时间
     * @param unit       时间单位
     * @param lockKey    锁的key
     * @param <R>        返回值类型
     * @param <ID>       id类型
     * @return 返回值
     */
    @Transactional(rollbackFor = Exception.class)
    public <R, ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallBack, Long time, TimeUnit unit, String lockKey, Long lockTime, TimeUnit lockTimeUnit) {
        // 拼接缓存key
        String key = keyPrefix + id;
        // 查询redis
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(json)) {
            return null;
        }
        // 转换为对象，反序列化对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        // 数据
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        // 逻辑过期时间
        LocalDateTime expireTime = redisData.getExpireTime();
        // 判断是否过期
        if (LocalDateTime.now().isBefore(expireTime)) {
            // 未过期，直接返回数据
            return r;
        }
        // 过期，获取互斥锁
        boolean lockResult = tryLock(lockKey, lockTime, lockTimeUnit);
        if (lockResult) {
            // 获取锁成功，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 查询数据库
                    R r1 = dbFallBack.apply(id);
                    // 写入redis
                    this.setWithLogicalExpire(key, r1, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 释放锁
                    unLock(lockKey);
                }

            });
        }
        // 返回过期旧数据
        return r;
    }


    /**
     * 尝试获取锁
     *
     * @param key        锁的key
     * @param expireTime 过期时间
     * @param timeUnit   时间单位
     * @return 是否获取锁
     */
    private boolean tryLock(String key, Long expireTime, TimeUnit timeUnit) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", expireTime, timeUnit);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放锁
     *
     * @param key 锁的key
     */
    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }
}
