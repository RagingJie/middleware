package com.study.redis.service.impl;

import cn.hutool.json.JSONUtil;
import com.study.redis.dto.Result;
import com.study.redis.entity.ShopType;
import com.study.redis.mapper.ShopTypeMapper;
import com.study.redis.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.redis.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryList() {
        // 1、现在redis中拿商铺类型
        String shopTypeListStr = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_TYPE_KEY);
        // 2、有缓存直接返回
        if (shopTypeListStr != null && !shopTypeListStr.isEmpty()) {
            List<ShopType> shopTypeList = JSONUtil.toList(shopTypeListStr, ShopType.class);
            return Result.ok(shopTypeList);
        }
        // 3、没有，查询数据库
        List<ShopType> shopTypeList = query().orderByAsc("sort").list();
        // 4、没有，返回错误
        if (shopTypeList == null) {
            return Result.fail("店铺类型不存在");
        }
        // 5、有，写入redis
        String jsonStr = JSONUtil.toJsonStr(shopTypeList);
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_TYPE_KEY, jsonStr);
        // 6、返回
        return Result.ok(shopTypeList);
    }
}
