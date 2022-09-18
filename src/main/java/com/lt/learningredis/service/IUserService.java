package com.lt.learningredis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lt.learningredis.dto.LoginFormDTO;
import com.lt.learningredis.dto.Result;
import com.lt.learningredis.entity.User;

import javax.servlet.http.HttpSession;

/**
 * 服务类
 *
 * @author teng
 */
public interface IUserService extends IService<User> {

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result sendCode(String phone, HttpSession session);

    Result sign();

    Result signCount();
}
