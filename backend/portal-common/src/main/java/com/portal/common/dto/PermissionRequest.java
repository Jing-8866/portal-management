package com.portal.common.dto;

import lombok.Data;

@Data
public class PermissionRequest {
    private Long userId;
    private Long subsystemId;
    private String permissionType;
}
