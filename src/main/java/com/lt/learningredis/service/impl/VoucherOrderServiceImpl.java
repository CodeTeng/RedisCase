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
        // 1. ????????????
        Long userId = voucherOrder.getUserId();
        // 2. ???????????????
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // 3. ?????????
        boolean isLock = lock.tryLock();
        // 4. ???????????????????????????
        if (!isLock) {
            // ??????????????? ?????????????????????
            log.error("?????????????????????");
        }
        try {
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            // ?????????
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
                    // 1. ???????????????????????????????????? xreadgroup group g1 c1 count 1 block 2000 streams stream.orders >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );
                    // 2. ??????????????????????????????
                    if (list == null || list.isEmpty()) {
                        // 2.1 ?????????????????????????????????????????????????????????
                        continue;
                    }
                    // 3. ??????????????????????????????
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> values = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                    // 4. ???????????? ???????????? ????????????
                    handleVoucherOrder(voucherOrder);
                    // 5. ACK?????? xack stream.orders g1 id
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    log.error("??????????????????", e);
                    handlePendingList();
                }
            }
        }

        private void handlePendingList() {
            while (true) {
                try {
                    // 1. ??????pending-list?????????????????? xreadgroup group g1 c1 count 1 block 2000 streams stream.orders 0
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(queueName, ReadOffset.from("0"))
                    );
                    // 2. ??????????????????????????????
                    if (list == null || list.isEmpty()) {
                        // 2.1 ?????????????????????pending-list?????????????????????????????????
                        break;
                    }
                    // 3. ??????????????????????????????
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> values = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                    // 4. ???????????? ???????????? ????????????
                    handleVoucherOrder(voucherOrder);
                    // 5. ACK?????? xack stream.orders g1 id
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    log.error("??????pending-list????????????", e);
                }
            }
        }
    }

    /**
     * ??????redis??????????????????
     *
     * @param voucherId ?????????id
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
        // 1. ??????Lua??????
        Long userId = UserHolder.getUser().getId();
        long orderId = redisIdWorker.nextId("order");
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(), String.valueOf(orderId)
        );
        // 2. ?????????????????????0
        int r = result.intValue();
        if (r != 0) {
            // 2.1 ??????0 ????????????????????????
            return Result.fail(r == 1 ? "????????????" : "??????????????????");
        }
        // 3. ??????????????????
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        // 4. ????????????id
        return Result.ok(orderId);
    }

//    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
//
//    private class VoucherOrderHandler implements Runnable {
//        @Override
//        public void run() {
//            while (true) {
//                try {
//                    // 1. ??????????????????????????????
//                    VoucherOrder voucherOrder = orderTasks.take();
//                    // 2. ????????????
//                    handleVoucherOrder(voucherOrder);
//                } catch (Exception e) {
//                    log.error("??????????????????", e);
//                }
//            }
//        }
//    }

//    /**
//     * ????????????????????????
//     *
//     * @param voucherId ?????????id
//     */
//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        // 1. ??????Lua??????
//        Long userId = UserHolder.getUser().getId();
//        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT, Collections.emptyList(), voucherId.toString(), userId.toString());
//        // 2. ?????????????????????0
//        int r = result.intValue();
//        if (r != 0) {
//            // 2.1 ??????0 ????????????????????????
//            return Result.fail(r == 1 ? "????????????" : "??????????????????");
//        }
//        // 2.2 ???0 ??????????????? ???????????????(??????id,??????id,?????????id)??????????????????
//        long orderId = redisIdWorker.nextId("order");
//        VoucherOrder voucherOrder = new VoucherOrder();
//        // 2.3 ??????id
//        voucherOrder.setId(orderId);
//        // 2.4 ??????id
//        voucherOrder.setUserId(userId);
//        // 2.5 ?????????id
//        voucherOrder.setVoucherId(voucherId);
//        // 2.6 ??????????????????
//        orderTasks.add(voucherOrder);
//
//        // 3. ??????????????????
//        proxy = (IVoucherOrderService) AopContext.currentProxy();
//        // 4. ????????????id
//        return Result.ok(orderId);
//    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        // 5. ??????????????????
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();
        // 5.1 ????????????
        Long count = this.query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 5.2 ??????????????????
        if (count > 0) {
            // ?????????????????????
            log.error("?????????????????????");
        }
        // 6. ????????????
        boolean isSuccess = seckillVoucherService.update().setSql("stock = stock - 1").eq("voucher_id", voucherId).gt("stock", 0).update();
        if (!isSuccess) {
            log.error("???????????????");
        }
        // 7 ????????????
        this.save(voucherOrder);
    }

    @Deprecated
    public Result old2seckillVoucher(Long voucherId) {
        // 1. ?????????????????????
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2. ????????????????????????
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("?????????????????????");
        }
        // 3. ????????????????????????
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("???????????????");
        }
        // 4. ????????????????????????
        if (voucher.getStock() < 1) {
            return Result.fail("???????????????");
        }
        return createVoucherOrder(voucherId);
    }

    @NotNull
    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        // 5. ????????????
        Long userId = UserHolder.getUser().getId();
        // ???????????????
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // ?????????????????????
        boolean isLock = lock.tryLock();
        if (!isLock) {
            // ???????????????
            return Result.fail("?????????????????????");
        }
        try {
            // 5.1 ????????????
            Long count = this.query().eq("user_id", userId).eq("voucher_id", voucherId).count();
            if (count > 0) {
                // 5.2 ??????????????????
                return Result.fail("?????????????????????");
            }
            // 6. ????????????
            boolean isSuccess = seckillVoucherService.update().setSql("stock = stock - 1").eq("voucher_id", voucherId).gt("stock", 0).update();
            if (!isSuccess) {
                return Result.fail("???????????????");
            }
            // 7. ????????????
            VoucherOrder voucherOrder = new VoucherOrder();
            // 7.1 ??????id
            long orderId = redisIdWorker.nextId("order");
            voucherOrder.setId(orderId);
            // 7.2 ??????id
            voucherOrder.setUserId(userId);
            // 7.3 ?????????id
            voucherOrder.setVoucherId(voucherId);
            // 7.4 ????????????
            this.save(voucherOrder);
            // 8. ????????????id
            return Result.ok(orderId);
        } finally {
            // ?????????
            lock.unlock();
        }
    }

    @Deprecated
    public Result old1seckillVoucher(Long voucherId) {
        // 1. ?????????????????????
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2. ????????????????????????
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            // ???????????????
            return Result.fail("?????????????????????");
        }
        // 3. ????????????????????????
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            // ???????????????
            return Result.fail("???????????????");
        }
        // 4. ????????????????????????
        if (voucher.getStock() < 1) {
            return Result.fail("???????????????");
        }
        // ??????????????? ????????????intern()?????????intern() ??????????????????????????????????????????,
        // ????????????????????????userId.toString() ????????????????????????????????????????????????new??????????????????
        // ???????????????????????????????????????????????????????????????????????????intern()??????,??????userId??????
        Long userId = UserHolder.getUser().getId();
        /*synchronized (userId.toString().intern()) {
            // ??????????????????(??????) ---> ???????????????
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            // ??????????????????????????????????????? ????????????
            return proxy.createVoucherOrder(voucherId);
        }*/
        // ?????????????????????????????????????????????
        // ???????????????
        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);

        // ??????redisson????????????
        // RLock lock = redissonClient.getLock("lock:order:" + userId);

        // ?????????
        boolean isLock = lock.tryLock(10L);
        // ???????????????????????????
        if (!isLock) {
            // ???????????? ?????????????????????
            return Result.fail("????????????????????????");
        }
        try {
            // ??????????????????(??????) ---> ???????????????
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            // ??????????????????????????????????????? ????????????
            return proxy.oldCreateVoucherOrder(voucherId);
        } finally {
            // ?????????
            lock.unlock();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Deprecated
    public Result oldCreateVoucherOrder(Long voucherId) {
        // ??????????????????
        // 5. ???????????????id?????????id????????????
        Long userId = UserHolder.getUser().getId();
        Long count = this.query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 5.1 ??????????????????
        if (count > 0) {
            // ?????????????????????
            return Result.fail("?????????????????????");
        }
        // 6. ????????????--->?????????????????????????????????
        /*boolean isSuccess = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .eq("stock", voucher.getStock()).update();*/
        // -->?????????????????????
        boolean isSuccess = seckillVoucherService.update().setSql("stock = stock - 1").eq("voucher_id", voucherId).gt("stock", 0).update();
        if (!isSuccess) {
            return Result.fail("???????????????");
        }
        // 7. ????????????
        VoucherOrder voucherOrder = new VoucherOrder();
        // 7.1 ??????id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 7.2 ??????id
        voucherOrder.setUserId(userId);
        // 7.3 ?????????id
        voucherOrder.setVoucherId(voucherId);
        // 7.4 ????????????
        this.save(voucherOrder);
        // 8. ????????????id
        return Result.ok(orderId);
    }
}
