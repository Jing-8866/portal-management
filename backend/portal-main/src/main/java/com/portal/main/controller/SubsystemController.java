package com.portal.main.controller;

import com.portal.common.annotation.OperationLog;
import com.portal.common.dto.ApiResult;
import com.portal.common.model.SysSubsystem;
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

    @GetMapping("/my")
    public ApiResult<List<SysSubsystem>> getMySubsystems() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) auth.getPrincipal();
        boolean isPlatformAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PLATFORM_ADMIN")
                        || a.getAuthority().equals("ROLE_SUBSYSTEM_ADMIN"));

        List<SysSubsystem> systems;
        if (isPlatformAdmin) {
            // 管理员看到所有系统（含停用的）
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
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "新增子系统", subsystem = "USER_MGMT")
    public ApiResult<Boolean> createSubsystem(@RequestBody SysSubsystem subsystem) {
        return ApiResult.success(subsystemService.createSubsystem(subsystem));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "修改子系统", subsystem = "USER_MGMT")
    public ApiResult<Boolean> updateSubsystem(@PathVariable Long id, @RequestBody SysSubsystem subsystem) {
        subsystem.setId(id);
        return ApiResult.success(subsystemService.updateSubsystem(subsystem));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "变更子系统状态", subsystem = "USER_MGMT")
    public ApiResult<Boolean> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return ApiResult.success(subsystemService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "删除子系统", subsystem = "USER_MGMT")
    public ApiResult<Boolean> deleteSubsystem(@PathVariable Long id) {
        return ApiResult.success(subsystemService.deleteSubsystem(id));
    }
}
