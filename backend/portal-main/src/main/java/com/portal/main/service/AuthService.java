package com.portal.main.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.portal.common.dto.*;
import com.portal.common.model.*;
import com.portal.common.util.JwtUtil;
import com.portal.main.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    @Autowired private SysUserMapper userMapper;
    @Autowired private SysUserRoleMapper userRoleMapper;
    @Autowired private com.portal.main.mapper.SysRoleSubsystemMapper roleSubsystemMapper;
    @Autowired private SysRoleMapper roleMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (user == null) throw new RuntimeException("用户不存在");
        if (user.getStatus() == 0) throw new RuntimeException("账户已被禁用");
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new RuntimeException("密码错误");

        // 确定用户最终权限级别（用于JWT和权限校验）
        // 规则：遍历用户所有角色，取最高权限级别
        //   - 有PLATFORM_ADMIN角色 → PLATFORM_ADMIN
        //   - 有SUBSYSTEM_ADMIN角色 → SUBSYSTEM_ADMIN
        //   - 通过任何角色拥有admin类型子系统权限 → SUBSYSTEM_ADMIN
        //   - 否则 → USER
        java.util.List<SysUserRole> userRoles = userRoleMapper.selectList(
            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
        String roleCode = "USER";
        if (userRoles != null && !userRoles.isEmpty()) {
            for (SysUserRole ur : userRoles) {
                SysRole role = roleMapper.selectById(ur.getRoleId());
                if (role != null) {
                    if ("PLATFORM_ADMIN".equals(role.getRoleCode())) { roleCode = "PLATFORM_ADMIN"; break; }
                    if ("SUBSYSTEM_ADMIN".equals(role.getRoleCode())) { roleCode = "SUBSYSTEM_ADMIN"; }
                }
            }
            // 如果仍是USER，检查是否通过角色拥有admin级别的子系统权限
            if ("USER".equals(roleCode)) {
                java.util.List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(java.util.stream.Collectors.toList());
                long adminPermCount = 0;
                try {
                    // 查询这些角色是否有admin类型的子系统权限
                    adminPermCount = roleIds.stream().mapToLong(rid -> {
                        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.portal.common.model.SysRoleSubsystem> w = 
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
                        w.eq(com.portal.common.model.SysRoleSubsystem::getRoleId, rid)
                         .eq(com.portal.common.model.SysRoleSubsystem::getPermissionType, "admin");
                        return roleSubsystemMapper.selectCount(w);
                    }).sum();
                } catch (Exception ignored) {}
                if (adminPermCount > 0) roleCode = "SUBSYSTEM_ADMIN";
            }
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), roleCode);
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRealName(), roleCode);
    }

    @Transactional
    public SysUser register(UserCreateRequest request) {
        SysUser existUser = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (existUser != null) throw new RuntimeException("用户名已存在");

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(1);
        // createdTime 和 updatedTime 不设置，让DB的DEFAULT CURRENT_TIMESTAMP生效
        // MyBatis Plus默认策略NOT_NULL：null字段不出现在INSERT SQL中
        userMapper.insert(user);

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(request.getRoleId());
        userRoleMapper.insert(userRole);
        return user;
    }

    public SysUser getUserById(Long id) {
        return userMapper.selectById(id);
    }

    public boolean updateProfile(Long userId, com.portal.main.dto.ProfileUpdateRequest request) {
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysUser> wrapper =
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, userId);

        boolean hasUpdate = false;
        if (request.getRealName() != null && !request.getRealName().isEmpty()) {
            wrapper.set(SysUser::getRealName, request.getRealName());
            hasUpdate = true;
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            wrapper.set(SysUser::getEmail, request.getEmail());
            hasUpdate = true;
        }
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            wrapper.set(SysUser::getPhone, request.getPhone());
            hasUpdate = true;
        }
        if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
            wrapper.set(SysUser::getPassword, passwordEncoder.encode(request.getNewPassword()));
            hasUpdate = true;
        }

        if (hasUpdate) {
            userMapper.update(null, wrapper);
        }
        return true;
    }
}
