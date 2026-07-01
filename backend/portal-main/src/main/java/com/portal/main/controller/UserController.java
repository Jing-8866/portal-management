package com.portal.main.controller;

import com.portal.common.annotation.OperationLog;
import com.portal.common.dto.ApiResult;
import com.portal.common.model.SysUser;
import com.portal.main.dto.UserDetailVO;
import com.portal.main.dto.ImportResult;
import com.portal.main.dto.UserCreateRequest;
import com.portal.main.dto.UserUpdateRequest;
import com.portal.main.service.UserService;
import com.portal.main.service.UserExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private UserExcelService userExcelService;

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    public ApiResult<List<SysUser>> getUserList(
            @RequestParam(required = false) String keyword) {
        List<SysUser> users = userService.getUserList(keyword);
        users.forEach(u -> u.setPassword(null));
        return ApiResult.success(users);
    }

    @GetMapping("/stats")
    public ApiResult<java.util.Map<String, Object>> getUserStats() {
        return ApiResult.success(userService.getUserStats());
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    public ApiResult<List<SysUser>> getAllUsers() {
        List<SysUser> users = userService.getAllUsers();
        users.forEach(u -> u.setPassword(null));
        return ApiResult.success(users);
    }

    @GetMapping("/no-permissions")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    public ApiResult<List<SysUser>> getUsersWithoutPermission(@RequestParam(required = false) String keyword) {
        return ApiResult.success(userService.getUsersWithoutPermission(keyword));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    public void exportUsers(javax.servlet.http.HttpServletResponse response,
                            @RequestParam(required = false) String ids) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=users.xlsx");
        java.util.List<Long> idList = null;
        if (ids != null && !ids.trim().isEmpty()) {
            idList = java.util.Arrays.stream(ids.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .map(Long::valueOf).collect(java.util.stream.Collectors.toList());
        }
        userExcelService.exportUsers(response.getOutputStream(), idList);
    }

    @GetMapping("/import/template")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    public void downloadTemplate(javax.servlet.http.HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=import_template.xlsx");
        userExcelService.generateTemplate(response.getOutputStream());
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "批量导入用户", subsystem = "USER_MGMT")
    public ApiResult<ImportResult> importUsers(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws Exception {
        return ApiResult.success(userExcelService.importUsers(file.getInputStream()));
    }

    @GetMapping("/{id}")
    public ApiResult<SysUser> getUserById(@PathVariable Long id) {
        SysUser user = userService.getUserById(id);
        if (user != null) user.setPassword(null);
        return ApiResult.success(user);
    }

    @GetMapping("/{id}/detail")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    public ApiResult<UserDetailVO> getUserDetail(@PathVariable Long id) {
        return ApiResult.success(userService.getUserDetail(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "新增用户", subsystem = "USER_MGMT")
    public ApiResult<SysUser> createUser(@RequestBody UserCreateRequest request) {
        try {
            SysUser user = userService.createUser(request);
            if (user != null) user.setPassword(null);
            return ApiResult.success(user);
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "更新用户信息", subsystem = "USER_MGMT")
    public ApiResult<Boolean> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return ApiResult.success(userService.updateUserWithRole(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "删除用户", subsystem = "USER_MGMT")
    public ApiResult<Boolean> deleteUser(@PathVariable Long id) {
        return ApiResult.success(userService.deleteUser(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "变更用户状态", subsystem = "USER_MGMT")
    public ApiResult<Boolean> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return ApiResult.success(userService.updateStatus(id, status));
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "重置用户密码", subsystem = "USER_MGMT")
    public ApiResult<Boolean> changePassword(@PathVariable Long id, @RequestBody com.portal.main.dto.ChangePasswordRequest request) {
        return ApiResult.success(userService.changePassword(id, request.getNewPassword()));
    }

    /**
     * 获取用户关联的所有角色ID列表
     */
    @GetMapping("/{id}/roles")
    public ApiResult<java.util.List<Long>> getUserRoles(@PathVariable Long id) {
        java.util.List<com.portal.common.model.SysUserRole> list = userService.getUserRoleList(id);
        java.util.List<Long> roleIds = list.stream()
            .map(com.portal.common.model.SysUserRole::getRoleId)
            .collect(java.util.stream.Collectors.toList());
        return ApiResult.success(roleIds);
    }
}