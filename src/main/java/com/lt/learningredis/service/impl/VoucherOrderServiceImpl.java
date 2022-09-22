package com.lt.learningredis.service.impl;

import cn.hutool.core.bean.BeanUtil;
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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    @Autowired
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        // 1. 获取用户
        Long userId = voucherOrder.getUserId();
        // 2. 创建锁对象
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // 3. 获取锁
        boolean isLock = lock.tryLock();
        // 4. 判断是否获取锁成功
        if (!isLock) {
            // 获取锁失败 返回错误或重试
            log.error("不允许重复下单");
        }
        try {
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    private IVoucherOrderService proxy;

    private class VoucherOrderHandler implements Runnable {
        String queueName = "stream.orders";

        @Override
        public void run() {
            while (true) {
                try {
                    // 1. 获取消息队列中的订单信息 xreadgroup group g1 c1 count 1 block 2000 streams stream.orders >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );
                    // 2. 判断消息获取是否成功
                    if (list == null || list.isEmpty()) {
                        // 2.1 获取失败，说明没有消息，继续下一次循环
                        continue;
                    }
                    // 3. 解析消息中的订单信息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> values = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                    // 4. 获取成功 可以下单 创建订单
                    handleVoucherOrder(voucherOrder);
                    // 5. ACK确认 xack stream.orders g1 id
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                    handlePendingList();
                }
            }
        }

        private void handlePendingList() {
            while (true) {
                try {
                    // 1. 获取pending-list中的订单信息 xreadgroup group g1 c1 count 1 block 2000 streams stream.orders 0
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(queueName, ReadOffset.from("0"))
                    );
                    // 2. 判断消息获取是否成功
                    if (list == null || list.isEmpty()) {
                        // 2.1 获取失败，说明pending-list没有异常消息，结束循环
                        break;
                    }
                    // 3. 解析消息中的订单信息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> values = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                    // 4. 获取成功 可以下单 创建订单
                    handleVoucherOrder(voucherOrder);
                    // 5. ACK确认 xack stream.orders g1 id
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理pending-list订单异常", e);
                }
            }
        }
    }

    /**
     * 利用redis消息队列实现
     *
     * @param voucherId 优惠卷id
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
        // 1. 执行Lua脚本
        Long userId = UserHolder.getUser().getId();
        long orderId = redisIdWorker.nextId("order");
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(), String.valueOf(orderId)
        );
        // 2. 判断结果是否为0
        int r = result.intValue();
        if (r != 0) {
            // 2.1 不为0 代表没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }
        // 3. 获取代理对象
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        // 4. 返回订单id
        return Result.ok(orderId);
    }

//    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
//
//    private class VoucherOrderHandler implements Runnable {
//        @Override
//        public void run() {
//            while (true) {
//                try {
//                    // 1. 获取队列中的订单信息
//                    VoucherOrder voucherOrder = orderTasks.take();
//                    // 2. 创建订单
//                    handleVoucherOrder(voucherOrder);
//                } catch (Exception e) {
//                    log.error("处理订单异常", e);
//                }
//            }
//        }
//    }

    /**
     * 利用阻塞队列实现
     *
     * @param voucherId 优惠卷id
     */
//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        // 1. 执行Lua脚本
//        Long userId = UserHolder.getUser().getId();
//        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT, Collections.emptyList(), voucherId.toString(), userId.toString());
//        // 2. 判断结果是否为0
//        int r = result.intValue();
//        if (r != 0) {
//            // 2.1 不为0 代表没有购买资格
//            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
//        }
//        // 2.2 为0 有购买资格 把下单信息(用户id,订单id,优惠卷id)存入阻塞队列
//        long orderId = redisIdWorker.nextId("order");
//        VoucherOrder voucherOrder = new VoucherOrder();
//        // 2.3 订单id
//        voucherOrder.setId(orderId);
//        // 2.4 用户id
//        voucherOrder.setUserId(userId);
//        // 2.5 代金券id
//        voucherOrder.setVoucherId(voucherId);
//        // 2.6 保存阻塞队列
//        orderTasks.add(voucherOrder);
//
//        // 3. 获取代理对象
//        proxy = (IVoucherOrderService) AopContext.currentProxy();
//        // 4. 返回订单id
//        return Result.ok(orderId);
//    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        // 5. 实现一人一单
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();
        // 5.1 查询订单
        Long count = this.query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 5.2 判断是否存在
        if (count > 0) {
            // 用户已经购买过
            log.error("不可再次抢购！");
        }
        // 6. 扣减库存
        boolean isSuccess = seckillVoucherService.update().setSql("stock = stock - 1").eq("voucher_id", voucherId).gt("stock", 0).update();
        if (!isSuccess) {
            log.error("库存不足！");
        }
        // 7 保存订单
        this.save(voucherOrder);
    }

    @Deprecated
    public Result old2seckillVoucher(Long voucherId) {
        // 1. 查询优惠卷信息
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2. 判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始！");
        }
        // 3. 判断秒杀是否结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已结束");
        }
        // 4. 判断库存是否充足
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足！");
        }
        return createVoucherOrder(voucherId);
    }

    @NotNull
    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        // 5. 一人一单
        Long userId = UserHolder.getUser().getId();
        // 创建锁对象
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // 尝试获取锁对象
        boolean isLock = lock.tryLock();
        if (!isLock) {
            // 获取锁失败
            return Result.fail("不允许重复下单");
        }
        try {
            // 5.1 查询订单
            Long count = this.query().eq("user_id", userId).eq("voucher_id", voucherId).count();
            if (count > 0) {
                // 5.2 用于已经买过
                return Result.fail("不允许重复下单");
            }
            // 6. 扣减库存
            boolean isSuccess = seckillVoucherService.update().setSql("stock = stock - 1").eq("voucher_id", voucherId).gt("stock", 0).update();
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
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    @Deprecated
    public Result old1seckillVoucher(Long voucherId) {
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

        // 改用redisson分布式锁
        // RLock lock = redissonClient.getLock("lock:order:" + userId);

        // 获取锁
        boolean isLock = lock.tryLock(10L);
        // 判断是否获取锁成功
        if (!isLock) {
            // 获取失败 返回错误或重试
            return Result.fail("不允许重复下单！");
        }
        try {
            // 获取代理对象(事务) ---> 使事务生效
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            // 将锁放在这里为了先执行事务 再释放锁
            return proxy.oldCreateVoucherOrder(voucherId);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Deprecated
    public Result oldCreateVoucherOrder(Long voucherId) {
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
        boolean isSuccess = seckillVoucherService.update().setSql("stock = stock - 1").eq("voucher_id", voucherId).gt("stock", 0).update();
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
