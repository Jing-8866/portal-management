package com.portal.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.portal.common.model.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {
    @Select("SELECT " +
            "COUNT(*) AS totalLogs, " +
            "SUM(CASE WHEN level='ERROR' OR level='FATAL' THEN 1 ELSE 0 END) AS errorCount, " +
            "SUM(CASE WHEN level='WARN' THEN 1 ELSE 0 END) AS warnCount, " +
            "SUM(CASE WHEN status=0 THEN 1 ELSE 0 END) AS failedCount " +
            "FROM sys_operation_log WHERE DATE(created_time) = CURDATE()")
    Map<String, Object> getTodayStats();

    @Select("SELECT DISTINCT subsystem_code FROM sys_operation_log WHERE subsystem_code IS NOT NULL AND subsystem_code != '' ORDER BY subsystem_code")
    List<String> selectDistinctSubsystemCodes();
}
