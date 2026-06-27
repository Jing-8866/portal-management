package com.portal.log.service;

import com.portal.common.model.SysOperationLog;
import com.portal.common.service.LogWriter;
import com.portal.log.mapper.SysOperationLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class LogWriterImpl implements LogWriter {

    @Autowired
    private SysOperationLogMapper logMapper;

    @Override
    @Async
    public void writeLog(SysOperationLog log) {
        try {
            // 确保所有NOT NULL字段有值
            if (!StringUtils.hasText(log.getLevel())) {
                log.setLevel("INFO");
            }
            if (log.getStatus() == null) {
                log.setStatus(1);
            }
            if (!StringUtils.hasText(log.getOperation())) {
                log.setOperation("未知操作");
            }
            // 多数据源下MetaObjectHandler可能不生效，手动设置时间
            if (log.getCreatedTime() == null) {
                log.setCreatedTime(LocalDateTime.now());
            }
            logMapper.insert(log);
        } catch (Exception e) {
            System.err.println("[LogWriter] 日志写入失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
