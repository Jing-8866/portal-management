package com.portal.main.controller;

import com.portal.common.annotation.OperationLog;
import com.portal.common.dto.ApiResult;
import com.portal.common.model.SysSubsystem;
import com.portal.main.service.SubsystemPermissionService;
import com.portal.main.service.SubsystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/subsystems")
public class SubsystemController {
    @Autowired
    private SubsystemService subsystemService;
    @Autowired
    private SubsystemPermissionService subsystemPermissionService;

    @GetMapping("/my")
    public ApiResult<List<SysSubsystem>> getMySubsystems() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) auth.getPrincipal();
        List<SysSubsystem> systems;
        if (subsystemPermissionService.isPlatformAdmin(userId)) {
            systems = subsystemService.getAllSubsystemsIncludeDisabled();
        } else {
            systems = subsystemService.getSubsystemsByUserId(userId);
        }
        return ApiResult.success(systems);
    }

    @GetMapping
    public ApiResult<List<SysSubsystem>> getAllSubsystems() {
        return ApiResult.success(subsystemService.getAllSubsystemsIncludeDisabled());
    }

    @PostMapping
    @PreAuthorize("@subsystemAuth.isPlatformAdmin()")
    @OperationLog(value = "新增子系统", subsystem = "USER_MGMT")
    public ApiResult<Boolean> createSubsystem(@RequestBody SysSubsystem subsystem) {
        return ApiResult.success(subsystemService.createSubsystem(subsystem));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@subsystemAuth.isPlatformAdmin()")
    @OperationLog(value = "修改子系统", subsystem = "USER_MGMT")
    public ApiResult<Boolean> updateSubsystem(@PathVariable Long id, @RequestBody SysSubsystem subsystem) {
        subsystem.setId(id);
        return ApiResult.success(subsystemService.updateSubsystem(subsystem));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@subsystemAuth.isPlatformAdmin()")
    @OperationLog(value = "变更子系统状态", subsystem = "USER_MGMT")
    public ApiResult<Boolean> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return ApiResult.success(subsystemService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@subsystemAuth.isPlatformAdmin()")
    @OperationLog(value = "删除子系统", subsystem = "USER_MGMT")
    public ApiResult<Boolean> deleteSubsystem(@PathVariable Long id) {
        return ApiResult.success(subsystemService.deleteSubsystem(id));
    }
}
