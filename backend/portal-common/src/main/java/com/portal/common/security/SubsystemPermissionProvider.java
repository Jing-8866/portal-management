package com.portal.common.security;

import java.util.List;

/**
 * 子系统权限校验（由 portal-main 实现，各业务模块注入使用）。
 */
public interface SubsystemPermissionProvider {

    /** 是否为平台管理员（拥有全部子系统权限） */
    boolean isPlatformAdmin(Long userId);

    /** 是否对指定子系统拥有 login 入口权限（显式 login 或 admin） */
    boolean hasLogin(Long userId, String subsystemCode);

    /** 是否对指定子系统拥有 query 查询权限（显式 query 或 admin） */
    boolean hasQuery(Long userId, String subsystemCode);

    /** 是否对指定子系统拥有 admin 管理权限 */
    boolean hasAdmin(Long userId, String subsystemCode);

    /** 是否对指定子系统 ID 拥有 admin 权限 */
    boolean hasAdminBySubsystemId(Long userId, Long subsystemId);

    /** 当前用户拥有 login 权限的子系统编码（含 admin 隐含入口） */
    List<String> getLoginSubsystemCodes(Long userId);

    /** 当前用户拥有 query 权限的子系统编码（含 admin 隐含查询） */
    List<String> getQuerySubsystemCodes(Long userId);

    /** 当前用户拥有 admin 权限的子系统编码 */
    List<String> getAdminSubsystemCodes(Long userId);
}
