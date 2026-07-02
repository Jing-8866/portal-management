package com.portal.dbmgmt.controller;

import com.portal.common.annotation.OperationLog;
import com.portal.common.dto.ApiResult;
import com.portal.dbmgmt.dto.SqlExecuteRequest;
import com.portal.dbmgmt.dto.SqlExecuteResult;
import com.portal.dbmgmt.model.BizDbInstance;
import com.portal.dbmgmt.service.DbInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/db")
public class DbInstanceController {
    @Autowired private DbInstanceService dbService;

    @GetMapping("/instances")
    @PreAuthorize("@subsystemAuth.hasQuery('DB_MGMT')")
    public ApiResult<List<BizDbInstance>> getAllInstances() {
        return ApiResult.success(dbService.getAllInstances());
    }

    @GetMapping("/stats")
    @PreAuthorize("@subsystemAuth.hasQuery('DB_MGMT')")
    public ApiResult<Map<String, Object>> getDbStats() {
        return ApiResult.success(dbService.getDbStats());
    }

    /** 批量刷新所有实例状态 */
    @PostMapping("/instances/refresh")
    @PreAuthorize("@subsystemAuth.hasQuery('DB_MGMT')")
    public ApiResult<Boolean> refreshInstances() {
        dbService.refreshAllInstances();
        return ApiResult.success(true);
    }

    /** 刷新单个实例状态 */
    @PostMapping("/instances/{id}/refresh")
    @PreAuthorize("@subsystemAuth.hasQuery('DB_MGMT')")
    public ApiResult<BizDbInstance> refreshSingleInstance(@PathVariable Long id) {
        return ApiResult.success(dbService.refreshSingleInstance(id));
    }

    @PostMapping("/instances/test-connection")
    @PreAuthorize("@subsystemAuth.hasQuery('DB_MGMT')")
    public ApiResult<Boolean> testConnection(@RequestBody BizDbInstance instance) {
        return ApiResult.success(dbService.testConnection(instance));
    }

    @PostMapping("/instances/{id}/execute-sql")
    @PreAuthorize("@subsystemAuth.hasAdmin('DB_MGMT')")
    @OperationLog(value = "执行SQL", subsystem = "DB_MGMT")
    public ApiResult<SqlExecuteResult> executeSql(@PathVariable Long id, @RequestBody SqlExecuteRequest request) {
        try {
            return ApiResult.success(dbService.executeSql(id, request.getSql()));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @GetMapping("/instances/{id}")
    @PreAuthorize("@subsystemAuth.hasQuery('DB_MGMT')")
    public ApiResult<BizDbInstance> getInstanceById(@PathVariable Long id) {
        return ApiResult.success(dbService.getInstanceById(id));
    }

    @PostMapping("/instances")
    @PreAuthorize("@subsystemAuth.hasAdmin('DB_MGMT')")
    @OperationLog(value = "新增数据库实例", subsystem = "DB_MGMT")
    public ApiResult<Boolean> createInstance(@RequestBody BizDbInstance instance) {
        try {
            return ApiResult.success(dbService.createInstance(instance));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @PutMapping("/instances/{id}")
    @PreAuthorize("@subsystemAuth.hasAdmin('DB_MGMT')")
    @OperationLog(value = "修改数据库实例", subsystem = "DB_MGMT")
    public ApiResult<Boolean> updateInstance(@PathVariable Long id, @RequestBody BizDbInstance instance) {
        instance.setId(id);
        try {
            return ApiResult.success(dbService.updateInstance(instance));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @DeleteMapping("/instances/{id}")
    @PreAuthorize("@subsystemAuth.hasAdmin('DB_MGMT')")
    @OperationLog(value = "删除数据库实例", subsystem = "DB_MGMT")
    public ApiResult<Boolean> deleteInstance(@PathVariable Long id) {
        return ApiResult.success(dbService.deleteInstance(id));
    }

    @GetMapping("/instances/{id}/tables")
    @PreAuthorize("@subsystemAuth.hasQuery('DB_MGMT')")
    public ApiResult<List<Map<String, Object>>> getTablesFromDb(@PathVariable Long id) {
        try { return ApiResult.success(dbService.getTablesFromDb(id)); }
        catch (RuntimeException e) { return ApiResult.error(e.getMessage()); }
    }

    @PostMapping("/instances/{id}/tables/refresh")
    @PreAuthorize("@subsystemAuth.hasQuery('DB_MGMT')")
    @OperationLog(value = "刷新表清单快照", subsystem = "DB_MGMT")
    public ApiResult<List<Map<String, Object>>> refreshTableSnapshot(@PathVariable Long id) {
        try { return ApiResult.success(dbService.refreshTableSnapshot(id)); }
        catch (RuntimeException e) { return ApiResult.error(e.getMessage()); }
    }

    @GetMapping("/instances/{id}/tables/{tableName}/structure")
    @PreAuthorize("@subsystemAuth.hasQuery('DB_MGMT')")
    public ApiResult<List<Map<String, Object>>> getTableStructure(@PathVariable Long id, @PathVariable String tableName) {
        try { return ApiResult.success(dbService.getTableStructure(id, tableName)); }
        catch (RuntimeException e) { return ApiResult.error(e.getMessage()); }
    }

    @GetMapping("/instances/{id}/tables/{tableName}/ddl")
    @PreAuthorize("@subsystemAuth.hasQuery('DB_MGMT')")
    public ApiResult<String> getTableDDL(@PathVariable Long id, @PathVariable String tableName) {
        try { return ApiResult.success(dbService.getTableDDL(id, tableName)); }
        catch (RuntimeException e) { return ApiResult.error(e.getMessage()); }
    }

    @GetMapping("/instances/{id}/realstats")
    @PreAuthorize("@subsystemAuth.hasQuery('DB_MGMT')")
    public ApiResult<Map<String, Object>> getDbStatsFromDb(@PathVariable Long id) {
        try { return ApiResult.success(dbService.getDbStatsFromDb(id)); }
        catch (RuntimeException e) { return ApiResult.error(e.getMessage()); }
    }
}
