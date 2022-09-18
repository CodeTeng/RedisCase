package com.lt.learningredis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author teng
 */
@SpringBootApplication
@MapperScan("com.lt.learningredis.mapper")
public class LearningredisApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearningredisApplication.class, args);
    }

}
