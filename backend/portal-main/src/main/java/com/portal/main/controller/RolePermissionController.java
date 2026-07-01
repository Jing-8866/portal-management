package com.portal.main.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.portal.common.annotation.OperationLog;
import com.portal.common.dto.ApiResult;
import com.portal.common.model.SysRoleSubsystem;
import com.portal.main.mapper.SysRoleSubsystemMapper;
import com.portal.main.service.SubsystemPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role-permissions")
public class RolePermissionController {

    @Autowired
    private SysRoleSubsystemMapper mapper;
    @Autowired
    private SubsystemPermissionService subsystemPermissionService;

    @GetMapping("/role/{roleId}")
    @PreAuthorize("@subsystemAuth.hasQuery('USER_MGMT')")
    public ApiResult<List<SysRoleSubsystem>> getRolePermissions(@PathVariable Long roleId) {
        return ApiResult.success(mapper.selectList(
            new LambdaQueryWrapper<SysRoleSubsystem>().eq(SysRoleSubsystem::getRoleId, roleId)));
    }

    @PostMapping("/grant")
    @PreAuthorize("@subsystemAuth.hasAdmin('USER_MGMT')")
    @OperationLog(value = "授予角色权限", subsystem = "USER_MGMT")
    public ApiResult<Boolean> grantPermission(@RequestBody SysRoleSubsystem perm) {
        try {
            assertCanManage(perm.getSubsystemId());
            LambdaQueryWrapper<SysRoleSubsystem> w = new LambdaQueryWrapper<>();
            w.eq(SysRoleSubsystem::getRoleId, perm.getRoleId())
             .eq(SysRoleSubsystem::getSubsystemId, perm.getSubsystemId())
             .eq(SysRoleSubsystem::getPermissionType, perm.getPermissionType());
            if (mapper.selectOne(w) != null) return ApiResult.success(true);
            return ApiResult.success(mapper.insert(perm) > 0);
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @DeleteMapping("/revoke")
    @PreAuthorize("@subsystemAuth.hasAdmin('USER_MGMT')")
    @OperationLog(value = "撤销角色权限", subsystem = "USER_MGMT")
    public ApiResult<Boolean> revokePermission(@RequestParam Long roleId, @RequestParam Long subsystemId, @RequestParam String permissionType) {
        try {
            assertCanManage(subsystemId);
            LambdaQueryWrapper<SysRoleSubsystem> w = new LambdaQueryWrapper<>();
            w.eq(SysRoleSubsystem::getRoleId, roleId)
             .eq(SysRoleSubsystem::getSubsystemId, subsystemId)
             .eq(SysRoleSubsystem::getPermissionType, permissionType);
            return ApiResult.success(mapper.delete(w) > 0);
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    private void assertCanManage(Long subsystemId) {
        if (!subsystemPermissionService.hasAdminBySubsystemId(currentUserId(), subsystemId)) {
            throw new RuntimeException("无权管理该子系统的权限");
        }
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
