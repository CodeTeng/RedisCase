package com.lt.learningredis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lt.learningredis.dto.Result;
import com.lt.learningredis.entity.VoucherOrder;

/**
 * @description:
 * @author: ~Teng~
 * @date: 2022/9/20 21:17
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {
    Result seckillVoucher(Long voucherId);

    Result oldCreateVoucherOrder(Long voucherId);

    void createVoucherOrder(VoucherOrder voucherOrder);
}
