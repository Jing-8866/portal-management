package com.portal.main.controller;

import com.portal.common.annotation.OperationLog;
import com.portal.common.dto.ApiResult;
import com.portal.common.dto.PermissionRequest;
import com.portal.common.model.SysUserSubsystem;
import com.portal.main.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {
    @Autowired
    private PermissionService permissionService;

    @PostMapping("/grant")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "分配权限", subsystem = "USER_MGMT")
    public ApiResult<Boolean> grantPermission(@RequestBody PermissionRequest request) {
        return ApiResult.success(permissionService.grantPermission(request));
    }

    @DeleteMapping("/revoke")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "撤销权限", subsystem = "USER_MGMT")
    public ApiResult<Boolean> revokePermission(@RequestParam Long userId, @RequestParam Long subsystemId, @RequestParam String permissionType) {
        return ApiResult.success(permissionService.revokePermission(userId, subsystemId, permissionType));
    }

    @GetMapping("/user/{userId}")
    public ApiResult<List<SysUserSubsystem>> getUserPermissions(@PathVariable Long userId) {
        return ApiResult.success(permissionService.getUserPermissions(userId));
    }

    @GetMapping("/subsystem/{subsystemId}")
    public ApiResult<List<SysUserSubsystem>> getSubsystemUsers(@PathVariable Long subsystemId) {
        return ApiResult.success(permissionService.getSubsystemUsers(subsystemId));
    }

    @Autowired
    private com.portal.main.mapper.SysSubsystemMapper subsystemMapper;

    /**
     * 获取用户的有效权限（合并直接分配 + 角色继承）
     */
    @GetMapping("/user/{userId}/effective")
    public ApiResult<java.util.List<java.util.Map<String, Object>>> getEffectivePermissions(@PathVariable Long userId) {
        return ApiResult.success(subsystemMapper.selectEffectivePermissions(userId));
    }
}
