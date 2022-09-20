package com.lt.learningredis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.lt.learningredis.constant.RedisConstants;
import com.lt.learningredis.dto.Result;
import com.lt.learningredis.entity.SeckillVoucher;
import com.lt.learningredis.entity.Voucher;
import com.lt.learningredis.mapper.SeckillVoucherMapper;
import com.lt.learningredis.mapper.VoucherMapper;
import com.lt.learningredis.service.IVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


/**
 * @author teng
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Autowired
    private SeckillVoucherMapper seckillVoucherMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private VoucherMapper voucherMapper;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        List<Voucher> vouchers = voucherMapper.queryVoucherOfShop(shopId);
        return Result.ok(vouchers);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSeckillVoucher(Voucher voucher) {
        // 添加优惠卷
        this.save(voucher);
        // 添加秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherMapper.insert(seckillVoucher);
        // 保存秒杀库存到redis中
        stringRedisTemplate.opsForValue().set(RedisConstants.SECKILL_STOCK_KEY + voucher.getId(), voucher.getStock().toString());
    }
}
