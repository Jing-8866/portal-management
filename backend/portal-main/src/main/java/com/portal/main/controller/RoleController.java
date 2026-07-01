package com.portal.main.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.portal.common.annotation.OperationLog;
import com.portal.common.dto.ApiResult;
import com.portal.common.model.SysRole;
import com.portal.main.dto.ImportResult;
import com.portal.main.mapper.SysRoleMapper;
import com.portal.main.service.RoleExcelService;
import com.portal.main.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private RoleExcelService roleExcelService;

    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize("@subsystemAuth.hasQuery('USER_MGMT')")
    public ApiResult<List<SysRole>> getAllRoles() {
        return ApiResult.success(roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId)));
    }

    @GetMapping("/export")
    @PreAuthorize("@subsystemAuth.hasAdmin('USER_MGMT')")
    public void exportRoles(javax.servlet.http.HttpServletResponse response,
                            @RequestParam(required = false) String ids) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=roles.xlsx");
        List<Long> idList = null;
        if (ids != null && !ids.trim().isEmpty()) {
            idList = java.util.Arrays.stream(ids.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .map(Long::valueOf).collect(java.util.stream.Collectors.toList());
        }
        roleExcelService.exportRoles(response.getOutputStream(), idList);
    }

    @GetMapping("/import/template")
    @PreAuthorize("@subsystemAuth.hasAdmin('USER_MGMT')")
    public void downloadTemplate(javax.servlet.http.HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=role_import_template.xlsx");
        roleExcelService.generateTemplate(response.getOutputStream());
    }

    @PostMapping("/import")
    @PreAuthorize("@subsystemAuth.hasAdmin('USER_MGMT')")
    @OperationLog(value = "批量导入角色", subsystem = "USER_MGMT")
    public ApiResult<ImportResult> importRoles(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws Exception {
        return ApiResult.success(roleExcelService.importRoles(file.getInputStream()));
    }

    @PostMapping
    @PreAuthorize("@subsystemAuth.hasAdmin('USER_MGMT')")
    @OperationLog(value = "新增角色", subsystem = "USER_MGMT")
    public ApiResult<Boolean> createRole(@RequestBody SysRole role) {
        if (role.getStatus() == null) role.setStatus(1);
        return ApiResult.success(roleMapper.insert(role) > 0);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@subsystemAuth.hasAdmin('USER_MGMT')")
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
    @PreAuthorize("@subsystemAuth.hasAdmin('USER_MGMT')")
    @OperationLog(value = "变更角色状态", subsystem = "USER_MGMT")
    public ApiResult<Boolean> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        LambdaUpdateWrapper<SysRole> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysRole::getId, id).set(SysRole::getStatus, status);
        return ApiResult.success(roleMapper.update(null, wrapper) > 0);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@subsystemAuth.hasAdmin('USER_MGMT')")
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
        Long protectedId = userService.getProtectedPlatformAdminUserId();
        List<Long> userIds = list.stream()
            .map(com.portal.common.model.SysUserRole::getUserId)
            .filter(uid -> protectedId == null || !protectedId.equals(uid))
            .collect(java.util.stream.Collectors.toList());
        return ApiResult.success(userIds);
    }

    /**
     * 批量设置角色的用户（覆盖式：先删除该角色所有关联，再重新插入）
     */
    @PostMapping("/{id}/users")
    @PreAuthorize("@subsystemAuth.hasAdmin('USER_MGMT')")
    @OperationLog(value = "角色用户分配", subsystem = "USER_MGMT")
    public ApiResult<Boolean> setRoleUsers(@PathVariable Long id, @RequestBody java.util.List<Long> userIds) {
        Long protectedId = userService.getProtectedPlatformAdminUserId();
        SysRole role = roleMapper.selectById(id);
        boolean isPlatformAdminRole = role != null && "PLATFORM_ADMIN".equals(role.getRoleCode());

        LambdaQueryWrapper<com.portal.common.model.SysUserRole> deleteWrapper =
            new LambdaQueryWrapper<com.portal.common.model.SysUserRole>().eq(com.portal.common.model.SysUserRole::getRoleId, id);
        if (isPlatformAdminRole && protectedId != null) {
            deleteWrapper.ne(com.portal.common.model.SysUserRole::getUserId, protectedId);
        }
        userRoleMapper.delete(deleteWrapper);

        if (userIds != null) {
            for (Long userId : userIds) {
                if (protectedId != null && protectedId.equals(userId)) {
                    if (!isPlatformAdminRole) {
                        continue;
                    }
                }
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
