package com.portal.order.util;

import com.portal.common.security.SubsystemPermissionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class OrderSecurityHelper {

    private static SubsystemPermissionProvider permissionProvider;

    @Autowired
    public OrderSecurityHelper(SubsystemPermissionProvider provider) {
        OrderSecurityHelper.permissionProvider = provider;
    }

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("未登录");
        }
        return (Long) auth.getPrincipal();
    }

    /** 是否对订单商城子系统拥有 admin 权限 */
    public static boolean isOrderAdmin() {
        if (permissionProvider == null) {
            return false;
        }
        return permissionProvider.hasAdmin(getCurrentUserId(), "ORDER_MGMT");
    }
}
