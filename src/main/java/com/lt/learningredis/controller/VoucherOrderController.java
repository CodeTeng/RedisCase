package com.lt.learningredis.controller;


import com.lt.learningredis.dto.Result;
import com.lt.learningredis.service.IVoucherOrderService;
import com.lt.learningredis.service.IVoucherService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;


/**
 * @author teng
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    @Resource
    private IVoucherOrderService voucherOrderService;

    /**
     * 实现优惠卷秒杀下单
     *
     * @param voucherId 优惠卷id
     * @return 订单id
     */
    @PostMapping("/seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        if (voucherId == null || voucherId <= 0) {
            return Result.fail("下单失败");
        }
        return voucherOrderService.seckillVoucher(voucherId);
    }
}
