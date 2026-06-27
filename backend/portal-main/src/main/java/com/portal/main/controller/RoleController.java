package com.portal.main.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.portal.common.annotation.OperationLog;
import com.portal.common.dto.ApiResult;
import com.portal.common.model.SysRole;
import com.portal.main.mapper.SysRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private SysRoleMapper roleMapper;

    @GetMapping
    public ApiResult<List<SysRole>> getAllRoles() {
        return ApiResult.success(roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "新增角色", subsystem = "USER_MGMT")
    public ApiResult<Boolean> createRole(@RequestBody SysRole role) {
        if (role.getStatus() == null) role.setStatus(1);
        return ApiResult.success(roleMapper.insert(role) > 0);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "修改角色", subsystem = "USER_MGMT")
    public ApiResult<Boolean> updateRole(@PathVariable Long id, @RequestBody SysRole role) {
        LambdaUpdateWrapper<SysRole> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysRole::getId, id);
        if (role.getRoleName() != null) wrapper.set(SysRole::getRoleName, role.getRoleName());
        if (role.getRoleCode() != null) wrapper.set(SysRole::getRoleCode, role.getRoleCode());
        if (role.getDescription() != null) wrapper.set(SysRole::getDescription, role.getDescription());
        if (role.getStatus() != null) wrapper.set(SysRole::getStatus, role.getStatus());
        return ApiResult.success(roleMapper.update(null, wrapper) > 0);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "变更角色状态", subsystem = "USER_MGMT")
    public ApiResult<Boolean> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        LambdaUpdateWrapper<SysRole> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysRole::getId, id).set(SysRole::getStatus, status);
        return ApiResult.success(roleMapper.update(null, wrapper) > 0);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "删除角色", subsystem = "USER_MGMT")
    public ApiResult<Boolean> deleteRole(@PathVariable Long id) {
        return ApiResult.success(roleMapper.deleteById(id) > 0);
    }

    /**
     * 获取某角色下的所有用户
     */
    @GetMapping("/{id}/users")
    public ApiResult<List<Long>> getRoleUsers(@PathVariable Long id) {
        List<com.portal.common.model.SysUserRole> list = userRoleMapper.selectList(
            new LambdaQueryWrapper<com.portal.common.model.SysUserRole>().eq(com.portal.common.model.SysUserRole::getRoleId, id));
        List<Long> userIds = list.stream().map(com.portal.common.model.SysUserRole::getUserId).collect(java.util.stream.Collectors.toList());
        return ApiResult.success(userIds);
    }

    /**
     * 批量设置角色的用户（覆盖式：先删除该角色所有关联，再重新插入）
     */
    @PostMapping("/{id}/users")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "角色用户分配", subsystem = "USER_MGMT")
    public ApiResult<Boolean> setRoleUsers(@PathVariable Long id, @RequestBody java.util.List<Long> userIds) {
        // 删除该角色的所有用户关联
        userRoleMapper.delete(new LambdaQueryWrapper<com.portal.common.model.SysUserRole>().eq(com.portal.common.model.SysUserRole::getRoleId, id));
        // 重新插入
        if (userIds != null) {
            for (Long userId : userIds) {
                com.portal.common.model.SysUserRole ur = new com.portal.common.model.SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(id);
                userRoleMapper.insert(ur);
            }
        }
        return ApiResult.success(true);
    }

    @Autowired
    private com.portal.main.mapper.SysUserRoleMapper userRoleMapper;
}
