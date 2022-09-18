package com.lt.learningredis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lt.learningredis.dto.Result;
import com.lt.learningredis.entity.ShopType;


/**
 * 服务类
 *
 * @author teng
 */
public interface IShopTypeService extends IService<ShopType> {

    Result queryTypeList();
}
