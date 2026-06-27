package com.portal.common.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class UserCreateRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
    private String realName;
    private String email;
    private String phone;
    @NotNull(message = "角色不能为空")
    private Long roleId;
}
