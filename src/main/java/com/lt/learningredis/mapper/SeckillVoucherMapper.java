package com.lt.learningredis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lt.learningredis.entity.SeckillVoucher;
import org.springframework.stereotype.Repository;


/**
 * 秒杀优惠券表，与优惠券是一对一关系 Mapper 接口
 *
 * @author teng
 */
@Repository
public interface SeckillVoucherMapper extends BaseMapper<SeckillVoucher> {

}
