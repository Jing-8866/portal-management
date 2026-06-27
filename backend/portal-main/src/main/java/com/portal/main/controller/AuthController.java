package com.portal.main.controller;

import com.portal.common.annotation.OperationLog;
import com.portal.common.dto.*;
import com.portal.common.model.SysUser;
import com.portal.main.service.AuthService;
import com.portal.main.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    @OperationLog(value = "用户登录", subsystem = "USER_MGMT")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            return ApiResult.success(authService.login(request));
        } catch (RuntimeException e) {
            return ApiResult.error(401, e.getMessage());
        }
    }

    @PostMapping("/register")
    @OperationLog(value = "用户注册", subsystem = "USER_MGMT")
    public ApiResult<SysUser> register(@Valid @RequestBody UserCreateRequest request) {
        try {
            SysUser user = authService.register(request);
            user.setPassword(null);
            return ApiResult.success("注册成功", user);
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public ApiResult<SysUser> getCurrentUserInfo() {
        org.springframework.security.core.Authentication auth =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) auth.getPrincipal();
        SysUser user = authService.getUserById(userId);
        if (user != null) user.setPassword(null);
        return ApiResult.success(user);
    }

    /**
     * 更新当前用户个人信息（包括密码）
     */
    @PutMapping("/profile")
    @com.portal.common.annotation.OperationLog(value = "修改个人信息", subsystem = "USER_MGMT")
    public ApiResult<Boolean> updateProfile(@RequestBody com.portal.main.dto.ProfileUpdateRequest request) {
        org.springframework.security.core.Authentication auth =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(authService.updateProfile(userId, request));
    }

    /**
     * 获取当前登录用户的详细信息（含角色+权限），无需管理员权限
     */
    @GetMapping("/me/detail")
    public ApiResult<com.portal.main.dto.UserDetailVO> getMyDetail() {
        org.springframework.security.core.Authentication auth =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(userService.getUserDetail(userId));
    }
}
