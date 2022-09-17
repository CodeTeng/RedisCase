package com.lt.learningredis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.lt.learningredis.dto.Result;
import com.lt.learningredis.entity.Voucher;
import com.lt.learningredis.mapper.VoucherMapper;
import com.lt.learningredis.service.IVoucherService;
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
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {
    @Override
    public Result queryVoucherOfShop(Long shopId) {
        return null;
    }

    @Override
    public void addSeckillVoucher(Voucher voucher) {

    }
}
