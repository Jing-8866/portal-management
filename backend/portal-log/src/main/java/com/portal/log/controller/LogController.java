package com.portal.log.controller;

import com.portal.common.annotation.OperationLog;
import com.portal.common.dto.ApiResult;
import com.portal.common.model.SysOperationLog;
import com.portal.log.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
public class LogController {
    @Autowired
    private LogService logService;

    @GetMapping("/all")
    @PreAuthorize("@subsystemAuth.hasQuery('LOG_ANALYSIS')")
    public ApiResult<List<SysOperationLog>> getAllLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String subsystemCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResult.success(logService.getAllLogs(level, subsystemCode, keyword, startDate, endDate));
    }

    @GetMapping("/subsystem-codes")
    @PreAuthorize("@subsystemAuth.hasQuery('LOG_ANALYSIS')")
    public ApiResult<List<String>> getSubsystemCodes() {
        return ApiResult.success(logService.getAllSubsystemCodes());
    }

    @GetMapping("/stats")
    @PreAuthorize("@subsystemAuth.hasQuery('LOG_ANALYSIS')")
    public ApiResult<Map<String, Object>> getTodayStats() {
        return ApiResult.success(logService.getTodayStats());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@subsystemAuth.hasAdmin('LOG_ANALYSIS')")
    @OperationLog(value = "删除日志", subsystem = "LOG_ANALYSIS")
    public ApiResult<Boolean> deleteLog(@PathVariable Long id) {
        return ApiResult.success(logService.deleteLog(id));
    }
}
