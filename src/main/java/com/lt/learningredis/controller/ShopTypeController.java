package com.lt.learningredis.controller;


import com.lt.learningredis.dto.Result;
import com.lt.learningredis.service.IShopTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;



/**
 * 前端控制器
 *
 * @author teng
 */
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    @GetMapping("/list")
    public Result queryTypeList() {
        return typeService.queryTypeList();
    }
}
