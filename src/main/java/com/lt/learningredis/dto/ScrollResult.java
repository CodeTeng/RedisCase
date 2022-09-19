package com.lt.learningredis.dto;

import lombok.Data;

import java.util.List;

/**
 * @description: 分页滚动查询返回结果
 * @author: ~Teng~
 * @date: 2022/9/19 20:42
 */
@Data
public class ScrollResult {
    private List<?> list;
    private Long minTime;
    private Integer offset;
}
