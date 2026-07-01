package com.portal.main.service;

import com.portal.common.model.SysSubsystem;
import com.portal.common.security.SubsystemPermissionProvider;
import com.portal.main.mapper.SysSubsystemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class SubsystemPermissionService implements SubsystemPermissionProvider {

    @Autowired
    private SysSubsystemMapper subsystemMapper;

    @Override
    public boolean isPlatformAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        return subsystemMapper.countPlatformAdminRole(userId) > 0;
    }

    @Override
    public boolean hasLogin(Long userId, String subsystemCode) {
        if (userId == null || subsystemCode == null) {
            return false;
        }
        if (isPlatformAdmin(userId)) {
            return true;
        }
        return subsystemMapper.countPermissionByCode(userId, subsystemCode, "login", "admin") > 0;
    }

    @Override
    public boolean hasQuery(Long userId, String subsystemCode) {
        if (userId == null || subsystemCode == null) {
            return false;
        }
        if (isPlatformAdmin(userId)) {
            return true;
        }
        return subsystemMapper.countPermissionByCode(userId, subsystemCode, "query", "admin") > 0;
    }

    @Override
    public boolean hasAdmin(Long userId, String subsystemCode) {
        if (userId == null || subsystemCode == null) {
            return false;
        }
        if (isPlatformAdmin(userId)) {
            return true;
        }
        return subsystemMapper.countPermissionByCode(userId, subsystemCode, "admin", "admin") > 0;
    }

    @Override
    public boolean hasAdminBySubsystemId(Long userId, Long subsystemId) {
        if (subsystemId == null) {
            return false;
        }
        SysSubsystem subsystem = subsystemMapper.selectById(subsystemId);
        if (subsystem == null) {
            return false;
        }
        return hasAdmin(userId, subsystem.getSystemCode());
    }

    @Override
    public List<String> getLoginSubsystemCodes(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        if (isPlatformAdmin(userId)) {
            return subsystemMapper.selectAllSubsystemCodes();
        }
        List<String> codes = subsystemMapper.selectLoginSubsystemCodes(userId);
        return codes != null ? codes : Collections.emptyList();
    }

    @Override
    public List<String> getQuerySubsystemCodes(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        if (isPlatformAdmin(userId)) {
            return subsystemMapper.selectAllSubsystemCodes();
        }
        List<String> codes = subsystemMapper.selectQuerySubsystemCodes(userId);
        return codes != null ? codes : Collections.emptyList();
    }

    @Override
    public List<String> getAdminSubsystemCodes(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        if (isPlatformAdmin(userId)) {
            return subsystemMapper.selectAllSubsystemCodes();
        }
        List<String> codes = subsystemMapper.selectAdminSubsystemCodes(userId);
        return codes != null ? codes : Collections.emptyList();
    }
}
