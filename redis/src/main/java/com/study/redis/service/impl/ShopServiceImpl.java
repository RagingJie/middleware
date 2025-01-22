package com.study.redis.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.study.redis.dto.Result;
import com.study.redis.entity.Shop;
import com.study.redis.mapper.ShopMapper;
import com.study.redis.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.redis.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public Result queryById(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        //1、从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2、判断是否存在，isNotBlank判断空字符串、null、"\t\n" 返回false
        if (StrUtil.isNotBlank(shopJson)) {
            //3、存在，直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        // 因为空字符串，也是不为null
        if (shopJson != null) {
            return Result.fail("店铺不存在");
        }
        //4、不存在，根据id查询数据库
        Shop shop = getById(id);
        //5、不存在，返回错误
        if (shop == null) {
            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return Result.fail("店铺不存在");
        }
        //6、存在，写入redis
        String shopStr = JSONUtil.toJsonStr(shop);
        stringRedisTemplate.opsForValue().set(key, shopStr, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //7、返回
        return Result.ok(shop);
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
