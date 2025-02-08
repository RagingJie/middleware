package com.study.redis.service.impl;

import com.study.redis.dto.Result;
import com.study.redis.entity.SeckillVoucher;
import com.study.redis.entity.VoucherOrder;
import com.study.redis.mapper.VoucherOrderMapper;
import com.study.redis.service.ISeckillVoucherService;
import com.study.redis.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.redis.utils.RedisIdWorker;
import com.study.redis.utils.SimpleRedisLock;
import com.study.redis.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 1、查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        if (voucher == null) {
            // 优惠券不存在
            return Result.fail("优惠券不存在！");
        }
        // 2、判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始！");
        }
        // 3、判断秒杀是否结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束！");
        }
        // 4、判断库存是否充足
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足！");
        }

        Long userId = UserHolder.getUser().getId();
      /*  synchronized (userId.toString().intern()) {  // 使用悲观锁，解决线程安全问题
            // 获取代理对象（事务）
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }*/

        // 使用分布式锁，实现线程安全
        SimpleRedisLock redisLock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        boolean lock = redisLock.tryLock(15L);
        if (!lock) {
            return Result.fail("不允许重复下单！");
        }

        try {
            // 获取代理对象（事务）
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            // 释放锁
            redisLock.unlock();
        }

    }

    /**
     * 创建订单
     *
     * @param voucherId 订单id
     * @return
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.DEFAULT, propagation = Propagation.REQUIRED)
    public Result createVoucherOrder(Long voucherId) {
        // 5、实现一人一单
        Long userId = UserHolder.getUser().getId();
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count > 0) {
            return Result.fail("您已经购买过一次了！");
        }
        // 6、扣减库存，使用乐观锁的思想，防止库存超卖
        boolean success = seckillVoucherService.update().setSql("stock = stock - 1").eq("voucher_id", voucherId).gt("stock", 0)  // 使用了乐观锁的思想，如果库存小于等于0，则更新失败
                .update();
        if (!success) {
            return Result.fail("库存不足！");
        }
        // 7、创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 7.1、生成订单号
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 7.2、用户id
//        Long userId = UserHolder.getUser().getId();
        voucherOrder.setUserId(userId);
        // 6.3、券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        // 7、返回订单id
        return Result.ok(orderId);
    }
}
