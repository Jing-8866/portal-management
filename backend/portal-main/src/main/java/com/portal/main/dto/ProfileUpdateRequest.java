package com.portal.main.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String realName;
    private String email;
    private String phone;
    /** 修改密码时填写，不修改则为空 */
    private String newPassword;
}
