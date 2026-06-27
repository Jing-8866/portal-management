package com.portal.main.dto;

import lombok.Data;

@Data
public class UserCreateRequest {
    private String username;
    private String realName;
    private String email;
    private String phone;
}
