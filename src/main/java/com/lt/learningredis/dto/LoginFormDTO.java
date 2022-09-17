package com.lt.learningredis.dto;

import lombok.Data;

/**
 * @author teng
 */
@Data
public class LoginFormDTO {
    private String phone;
    private String code;
    private String password;
}
