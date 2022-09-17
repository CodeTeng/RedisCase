package com.lt.learningredis.utils;

import com.lt.learningredis.dto.UserDTO;

/**
 * @description:
 * @author: ~Teng~
 * @date: 2022/9/17 22:05
 */
public class UserHolder {
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    public static void saveUser(UserDTO userDTO) {
        tl.set(userDTO);
    }

    public static UserDTO getUser() {
        return tl.get();
    }

    public static void removeUser() {
        tl.remove();
    }
}
