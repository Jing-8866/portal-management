package com.portal.log.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.portal.common.model.SysOperationLog;
import com.portal.log.mapper.SysOperationLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
public class LogService {
    @Autowired
    private SysOperationLogMapper logMapper;

    public List<SysOperationLog> getAllLogs(String level, String subsystemCode, String keyword, String startDate, String endDate) {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(level)) wrapper.eq(SysOperationLog::getLevel, level);
        if (StringUtils.hasText(subsystemCode)) wrapper.eq(SysOperationLog::getSubsystemCode, subsystemCode);
        if (StringUtils.hasText(keyword)) wrapper.like(SysOperationLog::getOperation, keyword);
        if (StringUtils.hasText(startDate)) wrapper.ge(SysOperationLog::getCreatedTime, startDate + " 00:00:00");
        if (StringUtils.hasText(endDate)) wrapper.le(SysOperationLog::getCreatedTime, endDate + " 23:59:59");
        wrapper.orderByDesc(SysOperationLog::getCreatedTime);
        return logMapper.selectList(wrapper);
    }

    public Map<String, Object> getTodayStats() {
        return logMapper.getTodayStats();
    }

    public List<String> getAllSubsystemCodes() {
        return logMapper.selectDistinctSubsystemCodes();
    }

    public boolean deleteLog(Long id) {
        return logMapper.deleteById(id) > 0;
    }
}
