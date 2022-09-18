package com.lt.learningredis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lt.learningredis.pojo.User;
import com.lt.learningredis.service.IShopService;
import com.lt.learningredis.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @description:
 * @author: ~Teng~
 * @date: 2022/9/17 20:08
 */
@SpringBootTest
public class RedisStringTests {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private ShopServiceImpl shopService;

    @Test
    void testSaveUser() throws JsonProcessingException {
        User user = new User("小腾", 20);
        // 手动序列化
        String json = MAPPER.writeValueAsString(user);
        stringRedisTemplate.opsForValue().set("user:100", json);

        String jsonUser = stringRedisTemplate.opsForValue().get("user:100");
        // 手动反序列化
        User user1 = MAPPER.readValue(jsonUser, User.class);
        System.out.println(user1);
    }

    @Test
    void testString() {
        stringRedisTemplate.opsForValue().set("name", "小腾");
        String name = stringRedisTemplate.opsForValue().get("name");
        System.out.println(name);
    }

    @Test
    void testSaveShop() {
        shopService.saveShopToRedis(1L, 10L);
    }
}
