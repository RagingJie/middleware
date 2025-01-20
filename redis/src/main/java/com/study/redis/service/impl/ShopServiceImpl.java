package com.study.redis.service.impl;

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
        //2、判断是否存在
        if (shopJson != null) {
            //3、存在，直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        //4、不存在，根据id查询数据库
        Shop shop = getById(id);
        //5、不存在，返回错误
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        //6、存在，写入redis
        String shopStr = JSONUtil.toJsonStr(shop);
        stringRedisTemplate.opsForValue().set(key, shopStr);
        //7、返回
        return Result.ok(shop);
    }
}
