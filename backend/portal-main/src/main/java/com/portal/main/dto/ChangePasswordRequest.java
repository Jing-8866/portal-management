package com.portal.main.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class ChangePasswordRequest {
    /** 新密码 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度需在6-50位之间")
    private String newPassword;
}
