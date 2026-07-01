package com.portal.main.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.portal.common.dto.PermissionRequest;
import com.portal.common.model.SysUserSubsystem;
import com.portal.common.util.PlatformAdminUser;
import com.portal.main.mapper.SysUserSubsystemMapper;
import com.portal.main.service.UserService;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionService {
    @Autowired private SysUserSubsystemMapper mapper;
    @Autowired private SubsystemPermissionService subsystemPermissionService;
    @Autowired private UserService userService;

    public boolean grantPermission(PermissionRequest request, Long operatorId) {
        assertNotProtectedTargetUser(request.getUserId());
        assertCanManageSubsystem(operatorId, request.getSubsystemId());
        LambdaQueryWrapper<SysUserSubsystem> w = new LambdaQueryWrapper<>();
        w.eq(SysUserSubsystem::getUserId, request.getUserId())
         .eq(SysUserSubsystem::getSubsystemId, request.getSubsystemId())
         .eq(SysUserSubsystem::getPermissionType, request.getPermissionType());
        if (mapper.selectOne(w) != null) return true;
        SysUserSubsystem p = new SysUserSubsystem();
        p.setUserId(request.getUserId());
        p.setSubsystemId(request.getSubsystemId());
        p.setPermissionType(request.getPermissionType());
        return mapper.insert(p) > 0;
    }

    public boolean revokePermission(Long userId, Long subsystemId, String permissionType, Long operatorId) {
        assertNotProtectedTargetUser(userId);
        assertCanManageSubsystem(operatorId, subsystemId);
        LambdaQueryWrapper<SysUserSubsystem> w = new LambdaQueryWrapper<>();
        w.eq(SysUserSubsystem::getUserId, userId)
         .eq(SysUserSubsystem::getSubsystemId, subsystemId)
         .eq(SysUserSubsystem::getPermissionType, permissionType);
        return mapper.delete(w) > 0;
    }

    private void assertCanManageSubsystem(Long operatorId, Long subsystemId) {
        if (!subsystemPermissionService.hasAdminBySubsystemId(operatorId, subsystemId)) {
            throw new RuntimeException("无权管理该子系统的权限");
        }
    }

    private void assertNotProtectedTargetUser(Long userId) {
        userService.assertNotProtectedPlatformAdmin(userId);
    }

    public List<SysUserSubsystem> getUserPermissions(Long userId) {
        return mapper.selectList(new LambdaQueryWrapper<SysUserSubsystem>().eq(SysUserSubsystem::getUserId, userId));
    }

    public List<SysUserSubsystem> getSubsystemUsers(Long subsystemId) {
        return mapper.selectList(new LambdaQueryWrapper<SysUserSubsystem>().eq(SysUserSubsystem::getSubsystemId, subsystemId));
    }
}
