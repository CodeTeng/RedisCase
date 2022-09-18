package com.lt.learningredis.constant;

/**
 * @author teng
 */
public interface RedisConstants {
    String LOGIN_CODE_KEY = "login:code:";
    Long LOGIN_CODE_TTL = 2L;
    String LOGIN_USER_KEY = "login:token:";
    Long LOGIN_USER_TTL = 30L;

    Long CACHE_NULL_TTL = 2L;

    Long CACHE_SHOP_TTL = 30L;
    String CACHE_SHOP_KEY = "cache:shop:";

    String CACHE_SHOP_TYPE_KEY = "cache:shop:type:list";

    String LOCK_SHOP_KEY = "lock:shop:";
    Long LOCK_SHOP_TTL = 10L;

    String SECKILL_STOCK_KEY = "seckill:stock:";

    String CACHE_BLOG_LIKED = "blog:liked:";

    String USER_SIGN_KEY = "sign:";
}
