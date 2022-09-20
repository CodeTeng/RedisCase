package com.lt.learningredis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lt.learningredis.entity.Voucher;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * @author teng
 */
@Repository
public interface VoucherMapper extends BaseMapper<Voucher> {

    List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);
}
