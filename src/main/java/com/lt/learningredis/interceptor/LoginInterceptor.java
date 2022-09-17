package com.lt.learningredis.interceptor;

import com.lt.learningredis.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

/**
 * @description: 登录拦截器
 * @author: ~Teng~
 * @date: 2022/9/17 22:04
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 判断ThreadLocal中是否存在用户
        if (Objects.isNull(UserHolder.getUser())) {
            // 需要拦截
            response.setStatus(401);
            return false;
        }
        // 有用户 放行
        return true;
    }
}
