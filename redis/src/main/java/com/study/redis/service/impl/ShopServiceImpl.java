package com.study.redis.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.study.redis.dto.Result;
import com.study.redis.entity.Shop;
import com.study.redis.mapper.ShopMapper;
import com.study.redis.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.redis.utils.CacheClient;
import com.study.redis.utils.RedisConstants;
import com.study.redis.utils.RedisData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    @Override
    public Result queryById(Long id) {
        // 缓存穿透
//        Shop shop = queryShopWithPassThrough(id);

        Shop shop = cacheClient.queryWithPassThrough(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 互斥锁解决缓存击穿
//        Shop shop = queryShopWithMutex(id);

        // 逻辑过期解决缓存击穿
//        Shop shop = queryShopWithLogicalExpire(id);

        if (shop == null) {
            return Result.fail("店铺不存在");
        }

        //7、返回
        return Result.ok(shop);
    }

    // 逻辑过期解决缓存击穿，
    public Shop queryShopWithLogicalExpire(Long id) {
        String shopKey = RedisConstants.CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        if (StrUtil.isBlank(shopJson)) {
            return null;
        }
        // 命中，先把json数据反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        // 未过期时，直接返回数据
        if (LocalDateTime.now().isBefore(expireTime)) {
            return shop;
        }
        // 过期时，开启新线程，重建缓存
        // 获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean lockResult = tryLock(lockKey);
        if (lockResult) {
            try {
                // 开启新线程，重建缓存
                CACHE_REBUILD_EXECUTOR.submit(() -> {
                    saveShopToRedis(id, 10L);
                });
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                // 释放锁
                unLock(lockKey);
            }
        }
        // 返回过期店铺信息
        return shop;
    }

    // 互斥锁解决缓冲击穿，可保证数据一致性，防止数据库压力，但会导致redis数据库压力过大(频繁的获取锁)，可用性降低，潜在死锁风险
    public Shop queryShopWithMutex(Long id) {
        String shopKey = RedisConstants.CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        if (StrUtil.isNotBlank(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // 空字符串
        if (shopJson != null) {
            return null;
        }
        Shop shop = null;
        // 实现缓存重建
        // 获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        try {
            boolean lockResult = tryLock(lockKey);
            // 判断是否获取锁成功
            if (!lockResult) {
                // 失败，则休眠重试
                Thread.sleep(50);
                // 递归获取锁
                queryShopWithMutex(id);
            }
            shop = getById(id);
            if (shop == null) {
                stringRedisTemplate.opsForValue().set(shopKey, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            shopJson = JSONUtil.toJsonStr(shop);
            stringRedisTemplate.opsForValue().set(shopKey, shopJson, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            unLock(lockKey);
        }
        return shop;
    }

    // 缓存穿透=》缓存与数据库中都没有要查询的数据
    public Shop queryShopWithPassThrough(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        //1、从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2、判断是否存在，isNotBlank判断空字符串、null、"\t\n" 返回false
        if (StrUtil.isNotBlank(shopJson)) {
            //3、存在，直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // 因为空字符串，也是不为null
        if (shopJson != null) {
            return null;
        }
        //4、不存在，根据id查询数据库
        Shop shop = getById(id);
        //5、不存在，返回错误
        if (shop == null) {
            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //6、存在，写入redis
        String shopStr = JSONUtil.toJsonStr(shop);
        stringRedisTemplate.opsForValue().set(key, shopStr, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return shop;
    }

    public void saveShopToRedis(Long id, Long expireSeconds) {
        // 1、查询商铺数据
        Shop shop = getById(id);
        // 2、封装过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        String jsonStr = JSONUtil.toJsonStr(redisData);
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, jsonStr);
    }

    // 尝试获取锁
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    // 释放锁
    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        if (shop.getId() == null) {
            return Result.fail("店铺id不能为空");
        }
        // 1、先更新数据库
        updateById(shop);
        // 2、再删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
