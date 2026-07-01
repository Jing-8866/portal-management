package com.portal.main.security;

import com.portal.common.security.SubsystemPermissionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Spring Security 表达式 Bean：
 * @PreAuthorize("@subsystemAuth.hasLogin('ORDER_MGMT')")
 * @PreAuthorize("@subsystemAuth.hasQuery('ORDER_MGMT')")
 * @PreAuthorize("@subsystemAuth.hasAdmin('ORDER_MGMT')")
 */
@Component("subsystemAuth")
public class SubsystemAuth {

    @Autowired
    private SubsystemPermissionProvider permissionProvider;

    public boolean hasLogin(String subsystemCode) {
        Long userId = currentUserId();
        return userId != null && permissionProvider.hasLogin(userId, subsystemCode);
    }

    public boolean hasQuery(String subsystemCode) {
        Long userId = currentUserId();
        return userId != null && permissionProvider.hasQuery(userId, subsystemCode);
    }

    public boolean hasAdmin(String subsystemCode) {
        Long userId = currentUserId();
        return userId != null && permissionProvider.hasAdmin(userId, subsystemCode);
    }

    public boolean isPlatformAdmin() {
        Long userId = currentUserId();
        return userId != null && permissionProvider.isPlatformAdmin(userId);
    }

    public boolean hasAdminBySubsystemId(Long subsystemId) {
        Long userId = currentUserId();
        return userId != null && permissionProvider.hasAdminBySubsystemId(userId, subsystemId);
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        return (Long) auth.getPrincipal();
    }
}
