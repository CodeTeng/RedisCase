package com.lt.learningredis.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lt.learningredis.constant.RedisConstants;
import com.lt.learningredis.dto.Result;
import com.lt.learningredis.entity.ShopType;
import com.lt.learningredis.mapper.ShopTypeMapper;
import com.lt.learningredis.service.IShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;


/**
 * 服务实现类
 *
 * @author teng
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        String cacheShopTypeJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_TYPE_KEY);
        if (StrUtil.isNotBlank(cacheShopTypeJson)) {
            List<ShopType> shopTypes = JSONUtil.toList(cacheShopTypeJson, ShopType.class);
            return Result.ok(shopTypes);
        }
        List<ShopType> shopTypes = this.query().orderByAsc("sort").list();
        if (shopTypes.isEmpty()) {
            return Result.fail("店铺类型不存在！");
        }
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_TYPE_KEY, JSONUtil.toJsonStr(shopTypes));
        return Result.ok(shopTypes);
    }
}
