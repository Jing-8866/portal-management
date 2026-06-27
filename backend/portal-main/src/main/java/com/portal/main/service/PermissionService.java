package com.portal.main.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.portal.common.dto.PermissionRequest;
import com.portal.common.model.SysUserSubsystem;
import com.portal.main.mapper.SysUserSubsystemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionService {
    @Autowired private SysUserSubsystemMapper mapper;

    public boolean grantPermission(PermissionRequest request) {
        LambdaQueryWrapper<SysUserSubsystem> w = new LambdaQueryWrapper<>();
        w.eq(SysUserSubsystem::getUserId, request.getUserId())
         .eq(SysUserSubsystem::getSubsystemId, request.getSubsystemId())
         .eq(SysUserSubsystem::getPermissionType, request.getPermissionType());
        if (mapper.selectOne(w) != null) return true;
        SysUserSubsystem p = new SysUserSubsystem();
        p.setUserId(request.getUserId());
        p.setSubsystemId(request.getSubsystemId());
        p.setPermissionType(request.getPermissionType());
        // createdTime让DB DEFAULT生效
        return mapper.insert(p) > 0;
    }

    public boolean revokePermission(Long userId, Long subsystemId, String permissionType) {
        LambdaQueryWrapper<SysUserSubsystem> w = new LambdaQueryWrapper<>();
        w.eq(SysUserSubsystem::getUserId, userId)
         .eq(SysUserSubsystem::getSubsystemId, subsystemId)
         .eq(SysUserSubsystem::getPermissionType, permissionType);
        return mapper.delete(w) > 0;
    }

    public List<SysUserSubsystem> getUserPermissions(Long userId) {
        return mapper.selectList(new LambdaQueryWrapper<SysUserSubsystem>().eq(SysUserSubsystem::getUserId, userId));
    }

    public List<SysUserSubsystem> getSubsystemUsers(Long subsystemId) {
        return mapper.selectList(new LambdaQueryWrapper<SysUserSubsystem>().eq(SysUserSubsystem::getSubsystemId, subsystemId));
    }
}
