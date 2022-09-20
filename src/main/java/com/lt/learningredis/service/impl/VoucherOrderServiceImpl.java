package com.lt.learningredis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lt.learningredis.dto.Result;
import com.lt.learningredis.entity.SeckillVoucher;
import com.lt.learningredis.entity.VoucherOrder;
import com.lt.learningredis.mapper.VoucherOrderMapper;
import com.lt.learningredis.service.ISeckillVoucherService;
import com.lt.learningredis.service.IVoucherOrderService;
import com.lt.learningredis.utils.RedisIdWorker;
import com.lt.learningredis.utils.UserHolder;
import com.lt.learningredis.utils.locks.SimpleRedisLock;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * @description:
 * @author: ~Teng~
 * @date: 2022/9/20 21:17
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    private RedisIdWorker redisIdWorker;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 1. 查询优惠卷信息
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2. 判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            // 秒杀未开始
            return Result.fail("秒杀尚未开始！");
        }
        // 3. 判断秒杀是否结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            // 秒杀已结束
            return Result.fail("秒杀已结束");
        }
        // 4. 判断库存是否充足
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足！");
        }
        // 添加悲观锁 需要加上intern()方法，intern() 这个方法是从常量池中拿到数据,
        // 如果我们直接使用userId.toString() 他拿到的对象实际上是不同的对象，new出来的对象，
        // 我们使用锁必须保证锁必须是同一把，所以我们需要使用intern()方法,确保userId一样
        Long userId = UserHolder.getUser().getId();
        /*synchronized (userId.toString().intern()) {
            // 获取代理对象(事务) ---> 使事务生效
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            // 将锁放在这里为了先执行事务 再释放锁
            return proxy.createVoucherOrder(voucherId);
        }*/
        // 改用自己创建的分布式锁进行测试
        // 创建锁对象
        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        // 获取锁
        boolean isLock = lock.tryLock(1200L);
        // 判断是否获取锁成功
        if (!isLock) {
            // 获取失败 返回错误或重试
            return Result.fail("不允许重复下单！");
        }
        try {
            // 获取代理对象(事务) ---> 使事务生效
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            // 将锁放在这里为了先执行事务 再释放锁
            return proxy.createVoucherOrder(voucherId);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    @NotNull
    @Transactional(rollbackFor = Exception.class)
    public Result createVoucherOrder(Long voucherId) {
        // 实现一人一单
        // 5. 根据优惠卷id和用户id查询订单
        Long userId = UserHolder.getUser().getId();
        Long count = this.query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 5.1 判断是否存在
        if (count > 0) {
            // 用户已经购买过
            return Result.fail("不可再次抢购！");
        }
        // 6. 扣减库存--->改用乐观锁解决超卖问题
        /*boolean isSuccess = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .eq("stock", voucher.getStock()).update();*/
        // -->再次更新乐观锁
        boolean isSuccess = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0).update();
        if (!isSuccess) {
            return Result.fail("库存不足！");
        }
        // 7. 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 7.1 订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 7.2 用户id
        voucherOrder.setUserId(userId);
        // 7.3 代金券id
        voucherOrder.setVoucherId(voucherId);
        // 7.4 保存订单
        this.save(voucherOrder);
        // 8. 返回订单id
        return Result.ok(orderId);
    }
}
