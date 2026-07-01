package com.portal.main.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.portal.common.model.*;
import com.portal.main.dto.UserDetailVO;
import com.portal.main.dto.UserCreateRequest;
import com.portal.main.dto.UserUpdateRequest;
import com.portal.main.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.List;

@Service
public class UserService {
    @Autowired private SysUserMapper userMapper;
    @Autowired private SysUserRoleMapper userRoleMapper;
    @Autowired private SysRoleMapper roleMapper;
    @Autowired private SysUserSubsystemMapper userSubsystemMapper;
    @Autowired private SysSubsystemMapper subsystemMapper;

    public List<SysUser> getUserList(String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getEmail, keyword);
        }
        wrapper.orderByAsc(SysUser::getId);
        return userMapper.selectList(wrapper);
    }

    public SysUser getUserById(Long id) { return userMapper.selectById(id); }

    public UserDetailVO getUserDetail(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) return null;

        UserDetailVO vo = new UserDetailVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());
        vo.setCreatedTime(user.getCreatedTime() != null ? user.getCreatedTime().toString() : null);

        // 用户可能有多个角色，取权限最高的（roleId最小）
        java.util.List<SysUserRole> userRoleList = userRoleMapper.selectList(
            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id).orderByAsc(SysUserRole::getRoleId));
        SysUserRole userRole = (userRoleList != null && !userRoleList.isEmpty()) ? userRoleList.get(0) : null;
        if (userRole != null) {
            vo.setRoleId(userRole.getRoleId());
            SysRole role = roleMapper.selectById(userRole.getRoleId());
            if (role != null) {
                vo.setRoleName(role.getRoleName());
                vo.setRoleCode(role.getRoleCode());
            }
        }

        // 获取有效权限（合并直接分配 + 角色继承，取并集）
        List<java.util.Map<String, Object>> effectivePerms = subsystemMapper.selectEffectivePermissions(id);
        List<SysUserSubsystem> permissions = new java.util.ArrayList<>();
        if (effectivePerms != null) {
            for (java.util.Map<String, Object> row : effectivePerms) {
                SysUserSubsystem p = new SysUserSubsystem();
                p.setUserId(id);
                Object sid = row.get("subsystem_id");
                p.setSubsystemId(sid instanceof Number ? ((Number) sid).longValue() : Long.valueOf(sid.toString()));
                p.setPermissionType((String) row.get("permission_type"));
                permissions.add(p);
            }
        }
        vo.setPermissions(permissions);
        return vo;
    }

    /**
     * 新增用户，默认密码 123456，不自动分配角色
     */
    @Transactional
    public SysUser createUser(UserCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername())) {
            throw new RuntimeException("用户名不能为空");
        }
        String username = request.getUsername().trim();
        SysUser exists = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (exists != null) {
            throw new RuntimeException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(org.springframework.security.crypto.bcrypt.BCrypt.hashpw(
                "123456", org.springframework.security.crypto.bcrypt.BCrypt.gensalt()));
        user.setRealName(StringUtils.hasText(request.getRealName()) ? request.getRealName().trim() : null);
        user.setEmail(StringUtils.hasText(request.getEmail()) ? request.getEmail().trim() : null);
        user.setPhone(StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null);
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    /**
     * 更新用户信息（含角色修改）
     */
    @Transactional
    public boolean updateUserWithRole(Long id, UserUpdateRequest request) {
        // 更新基本信息
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, id);
        boolean hasUpdate = false;
        if (StringUtils.hasText(request.getRealName())) {
            wrapper.set(SysUser::getRealName, request.getRealName());
            hasUpdate = true;
        }
        if (StringUtils.hasText(request.getEmail())) {
            wrapper.set(SysUser::getEmail, request.getEmail());
            hasUpdate = true;
        }
        if (StringUtils.hasText(request.getPhone())) {
            wrapper.set(SysUser::getPhone, request.getPhone());
            hasUpdate = true;
        }
        if (request.getStatus() != null) {
            wrapper.set(SysUser::getStatus, request.getStatus());
            hasUpdate = true;
        }
        if (hasUpdate) {
            userMapper.update(null, wrapper);
        }

        // 更新角色（如果传了roleId）
        if (request.getRoleId() != null) {
            // 删除旧角色关联
            userRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id)
            );
            // 插入新角色关联
            SysUserRole newRole = new SysUserRole();
            newRole.setUserId(id);
            newRole.setRoleId(request.getRoleId());
            userRoleMapper.insert(newRole);
        }

        return true;
    }

    public boolean updateUser(SysUser user) {
        if (user.getId() == null) return false;
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, user.getId());
        if (StringUtils.hasText(user.getRealName())) wrapper.set(SysUser::getRealName, user.getRealName());
        if (StringUtils.hasText(user.getEmail())) wrapper.set(SysUser::getEmail, user.getEmail());
        if (StringUtils.hasText(user.getPhone())) wrapper.set(SysUser::getPhone, user.getPhone());
        if (user.getStatus() != null) wrapper.set(SysUser::getStatus, user.getStatus());
        return userMapper.update(null, wrapper) > 0;
    }

    public boolean deleteUser(Long id) { return userMapper.deleteById(id) > 0; }

    public boolean updateStatus(Long id, Integer status) {
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, id).set(SysUser::getStatus, status);
        return userMapper.update(null, wrapper) > 0;
    }

    public List<SysUser> getAllUsers() {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>().orderByAsc(SysUser::getId));
    }

    public List<SysUser> getUsersWithoutPermission(String keyword) {
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        List<SysUser> users = userMapper.selectUsersWithoutPermission(kw);
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    public boolean changePassword(Long id, String newPassword) {
        SysUser user = userMapper.selectById(id);
        if (user == null) return false;
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, id)
               .set(SysUser::getPassword, org.springframework.security.crypto.bcrypt.BCrypt.hashpw(newPassword, org.springframework.security.crypto.bcrypt.BCrypt.gensalt()));
        return userMapper.update(null, wrapper) > 0;
    }

    public java.util.Map<String, Object> getUserStats() {
        long total = userMapper.selectCount(null);
        long enabled = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, 1));
        long disabled = total - enabled;
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("total", total);
        stats.put("enabled", enabled);
        stats.put("disabled", disabled);
        return stats;
    }

    public java.util.List<SysUserRole> getUserRoleList(Long userId) {
        return userRoleMapper.selectList(
            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
    }
}