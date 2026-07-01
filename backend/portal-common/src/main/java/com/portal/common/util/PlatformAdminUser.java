package com.portal.common.util;

import com.portal.common.model.SysUser;

/**
 * 内置系统管理员账号（username=admin）保护规则。
 */
public final class PlatformAdminUser {

    public static final String USERNAME = "admin";

    private PlatformAdminUser() {
    }

    public static boolean isProtectedUsername(String username) {
        return username != null && USERNAME.equalsIgnoreCase(username.trim());
    }

    public static boolean isProtectedUser(SysUser user) {
        return user != null && isProtectedUsername(user.getUsername());
    }
}
