package com.portal.main.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.portal.common.annotation.OperationLog;
import com.portal.common.dto.ApiResult;
import com.portal.common.model.SysRoleSubsystem;
import com.portal.main.mapper.SysRoleSubsystemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/role-permissions")
public class RolePermissionController {

    @Autowired
    private SysRoleSubsystemMapper mapper;

    /**
     * 获取某角色的子系统权限
     */
    @GetMapping("/role/{roleId}")
    public ApiResult<List<SysRoleSubsystem>> getRolePermissions(@PathVariable Long roleId) {
        return ApiResult.success(mapper.selectList(
            new LambdaQueryWrapper<SysRoleSubsystem>().eq(SysRoleSubsystem::getRoleId, roleId)));
    }

    /**
     * 授予角色子系统权限
     */
    @PostMapping("/grant")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "授予角色权限", subsystem = "USER_MGMT")
    public ApiResult<Boolean> grantPermission(@RequestBody SysRoleSubsystem perm) {
        LambdaQueryWrapper<SysRoleSubsystem> w = new LambdaQueryWrapper<>();
        w.eq(SysRoleSubsystem::getRoleId, perm.getRoleId())
         .eq(SysRoleSubsystem::getSubsystemId, perm.getSubsystemId())
         .eq(SysRoleSubsystem::getPermissionType, perm.getPermissionType());
        if (mapper.selectOne(w) != null) return ApiResult.success(true);
        return ApiResult.success(mapper.insert(perm) > 0);
    }

    /**
     * 撤销角色子系统权限
     */
    @DeleteMapping("/revoke")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "撤销角色权限", subsystem = "USER_MGMT")
    public ApiResult<Boolean> revokePermission(@RequestParam Long roleId, @RequestParam Long subsystemId, @RequestParam String permissionType) {
        LambdaQueryWrapper<SysRoleSubsystem> w = new LambdaQueryWrapper<>();
        w.eq(SysRoleSubsystem::getRoleId, roleId)
         .eq(SysRoleSubsystem::getSubsystemId, subsystemId)
         .eq(SysRoleSubsystem::getPermissionType, permissionType);
        return ApiResult.success(mapper.delete(w) > 0);
    }
}
