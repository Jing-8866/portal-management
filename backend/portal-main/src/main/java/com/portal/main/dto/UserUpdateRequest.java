package com.portal.main.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String realName;
    private String email;
    private String phone;
    private Integer status;
    /** 角色ID（可选，传了则更新角色） */
    private Long roleId;
}
