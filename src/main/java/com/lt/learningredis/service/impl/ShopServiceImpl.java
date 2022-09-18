package com.lt.learningredis.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lt.learningredis.constant.RedisConstants;
import com.lt.learningredis.dto.RedisData;
import com.lt.learningredis.dto.Result;
import com.lt.learningredis.entity.Shop;
import com.lt.learningredis.mapper.ShopMapper;
import com.lt.learningredis.service.IShopService;
import com.lt.learningredis.utils.CacheClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * @author teng
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    /**
     * 线程池
     */
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    @Override
    public Result queryShopById(Long id) {
        // 用工具类解决缓存穿透
        Shop shop = cacheClient
                .queryWithPassThrough(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return Result.ok(shop);
    }

    /**
     * 利用逻辑过期时间解决缓存击穿
     */
    private Shop queryWithLogicalExpire(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        // 1. 从redis中查询
        String redisDataJson = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(redisDataJson)) {
            // 2. 不存在 直接返回空
            return null;
        }
        // 3. 命中缓存 判断是否过期
        RedisData redisData = JSONUtil.toBean(redisDataJson, RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        Shop shop = JSONUtil.toBean(data, Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())) {
            // 未过期
            return shop;
        }
        // 4. 过期 获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        if (isLock) {
            // 获取成功 开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    this.saveShopToRedis(id, 20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 释放锁
                    unlock(lockKey);
                }
            });
        }
        return shop;
    }

    /**
     * 将数据封装逻辑过期时间保存到Redis中
     */
    public void saveShopToRedis(Long id, Long expireSeconds) {
        Shop shop = this.getById(id);
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 利用互斥锁解决缓存击穿
     *
     * @param id 商铺id
     * @return 商铺
     */
    private Shop queryWithMutex(Long id) {
        // 1. 从redis中查询缓存
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        String cacheShopJson = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(cacheShopJson)) {
            // 命中 直接返回
            return JSONUtil.toBean(cacheShopJson, Shop.class);
        }
        // 判断是否为空值
        if (cacheShopJson != null) {
            return null;
        }
        // 2. 获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            if (!isLock) {
                // 获取失败 休眠重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            // 成功 根据id查询数据库
            shop = this.getById(id);
            // 3. 判断数据库中是否存在
            if (Objects.isNull(shop)) {
                // 不存在 存入空对象 防止缓存穿透
                stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            // 4. 查询到 写入redis中 并设置过期时间
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 5. 释放互斥锁
            unlock(lockKey);
        }
        return shop;
    }

    /**
     * 获取互斥锁
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10L, TimeUnit.SECONDS);
        // 不要直接返回，因为有自动拆箱-防止空指针
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放互斥锁
     */
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 缓存穿透---缓存空对象解决方案
     */
    @NotNull
    private Shop queryWithPassThrough(Long id) {
        // 1. 从redis中查询
        String cacheShopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        // 2. 判断是否存在
        if (StrUtil.isNotBlank(cacheShopJson)) {
            return JSONUtil.toBean(cacheShopJson, Shop.class);
        }
        // 2.1 命中的是否是空值
        if (cacheShopJson != null) {
            return null;
        }
        // 3. 不存在，根据id从数据库查询
        Shop shop = this.getById(id);
        // 4. 没有返回未查询到
        if (Objects.isNull(shop)) {
            // 4.1 将空值写入redis
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 5. 数据库中查询到，返回并存入缓存，并且设置超时时间
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return shop;
    }

    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        // 1. 修改数据库
        this.updateById(shop);
        // 2. 删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
