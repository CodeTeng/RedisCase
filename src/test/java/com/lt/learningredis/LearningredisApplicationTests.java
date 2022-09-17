package com.lt.learningredis;

import com.lt.learningredis.pojo.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

@SpringBootTest
class LearningredisApplicationTests {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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

}
