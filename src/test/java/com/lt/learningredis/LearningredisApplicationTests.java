package com.lt.learningredis;

import com.lt.learningredis.constant.RedisConstants;
import com.lt.learningredis.entity.Shop;
import com.lt.learningredis.pojo.User;
import com.lt.learningredis.service.impl.ShopServiceImpl;
import kotlin.collections.CollectionsKt;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SpringBootTest
class LearningredisApplicationTests {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ShopServiceImpl shopService;

    @Test
    void testString() {
        redisTemplate.opsForValue().set("name", "虎哥");
        Object name = redisTemplate.opsForValue().get("name");
        System.out.println("name = " + name);
    }

    @Test
    void testSaveUser() {
        redisTemplate.opsForValue().set("user:100", new User("小李", 20));
        User user = (User) redisTemplate.opsForValue().get("user:100");
        System.out.println(user);
    }

    @Test
    void testHash() {
        redisTemplate.opsForHash().put("user:200", "name", "小腾");
        redisTemplate.opsForHash().put("user:200", "age", "20");
        Map<Object, Object> entries = redisTemplate.opsForHash().entries("user:200");
        for (Object key : entries.keySet()) {
            System.out.println("key = " + key);
            System.out.println("value = " + entries.get(key));
        }
    }

    @Test
    void testHyperLogLog() {
        String[] values = new String[1000];
        int j = 0;
        for (int i = 0; i < 1000000; i++) {
            j = i % 1000;
            values[j] = "user_" + i;
            if (j == 999) {
                // 发送到redis中
                stringRedisTemplate.opsForHyperLogLog().add("hll", values);
            }
        }
        // 统计数量
        Long size = stringRedisTemplate.opsForHyperLogLog().size("hll");
        System.out.println(size);
    }

    @Test
    void loadShopData() {
        // 1. 查询店铺信息
        List<Shop> shopList = shopService.list();
        // 2. 把店铺分组，按照typeId分组，typeId一致的放到一个集合
        Map<Long, List<Shop>> map = shopList.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        // 3. 存入redis中 geoadd key 经度 维度 member
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            // 3.1.获取类型id
            Long typeId = entry.getKey();
            String key = RedisConstants.SHOP_GEO_KEY + typeId;
            // 3.2.获取同类型的店铺的集合
            List<Shop> value = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>();
            for (Shop shop : value) {
                // 效率比较低--->改为批量插入
                // stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());
                locations.add(new RedisGeoCommands.GeoLocation<>(shop.getId().toString(), new Point(shop.getX(), shop.getY())));
            }
            stringRedisTemplate.opsForGeo().add(key, locations);
        }
    }
}
