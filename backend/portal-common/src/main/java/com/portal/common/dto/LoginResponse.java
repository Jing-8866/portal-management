package com.portal.common.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String roleCode;
    /** 拥有 login 入口权限的子系统编码列表（含 admin 隐含） */
    private java.util.List<String> loginSubsystemCodes;
    /** 拥有 query 查询权限的子系统编码列表（含 admin 隐含） */
    private java.util.List<String> querySubsystemCodes;
    /** 拥有 admin 管理权限的子系统编码列表 */
    private java.util.List<String> adminSubsystemCodes;
}
