package com.portal.common.service;

import com.portal.common.model.SysOperationLog;

/**
 * 日志写入接口 - 在common中定义，由portal-log模块实现
 * 解耦AOP切面与日志模块的直接依赖
 */
public interface LogWriter {
    void writeLog(SysOperationLog log);
}
