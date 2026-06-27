package com.portal.main.dto;

import com.portal.common.model.SysUser;
import com.portal.common.model.SysUserSubsystem;
import lombok.Data;

import java.util.List;

@Data
public class UserDetailVO {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private Integer status;
    private String createdTime;

    /** 角色ID */
    private Long roleId;
    /** 角色名称 */
    private String roleName;
    /** 角色编码 */
    private String roleCode;

    /** 用户的子系统权限列表 */
    private List<SysUserSubsystem> permissions;
}
