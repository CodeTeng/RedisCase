package com.lt.learningredis.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description:
 * @author: ~Teng~
 * @date: 2022/9/17 20:02
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private String name;

    private Integer age;
}
