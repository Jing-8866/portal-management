package com.portal.main.controller;

import com.portal.common.annotation.OperationLog;
import com.portal.common.dto.ApiResult;
import com.portal.common.dto.PermissionRequest;
import com.portal.common.model.SysUserSubsystem;
import com.portal.main.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {
    @Autowired
    private PermissionService permissionService;

    @PostMapping("/grant")
    @PreAuthorize("@subsystemAuth.hasAdmin('USER_MGMT')")
    @OperationLog(value = "分配权限", subsystem = "USER_MGMT")
    public ApiResult<Boolean> grantPermission(@RequestBody PermissionRequest request) {
        try {
            return ApiResult.success(permissionService.grantPermission(request, currentUserId()));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @DeleteMapping("/revoke")
    @PreAuthorize("@subsystemAuth.hasAdmin('USER_MGMT')")
    @OperationLog(value = "撤销权限", subsystem = "USER_MGMT")
    public ApiResult<Boolean> revokePermission(@RequestParam Long userId, @RequestParam Long subsystemId, @RequestParam String permissionType) {
        try {
            return ApiResult.success(permissionService.revokePermission(userId, subsystemId, permissionType, currentUserId()));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@subsystemAuth.hasQuery('USER_MGMT')")
    public ApiResult<List<SysUserSubsystem>> getUserPermissions(@PathVariable Long userId) {
        return ApiResult.success(permissionService.getUserPermissions(userId));
    }

    @GetMapping("/subsystem/{subsystemId}")
    @PreAuthorize("@subsystemAuth.hasQuery('USER_MGMT')")
    public ApiResult<List<SysUserSubsystem>> getSubsystemUsers(@PathVariable Long subsystemId) {
        return ApiResult.success(permissionService.getSubsystemUsers(subsystemId));
    }

    @Autowired
    private com.portal.main.mapper.SysSubsystemMapper subsystemMapper;

    @GetMapping("/user/{userId}/effective")
    @PreAuthorize("@subsystemAuth.hasQuery('USER_MGMT')")
    public ApiResult<java.util.List<java.util.Map<String, Object>>> getEffectivePermissions(@PathVariable Long userId) {
        return ApiResult.success(subsystemMapper.selectEffectivePermissions(userId));
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
