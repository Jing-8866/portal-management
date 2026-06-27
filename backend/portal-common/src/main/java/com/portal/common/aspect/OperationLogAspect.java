package com.portal.common.aspect;

import com.portal.common.annotation.OperationLog;
import com.portal.common.model.SysOperationLog;
import com.portal.common.service.LogWriter;
import com.portal.common.util.JwtUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Aspect
@Component
public class OperationLogAspect {

    @Autowired(required = false)
    private LogWriter logWriter;

    @Autowired
    private JwtUtil jwtUtil;

    @Around("@annotation(com.portal.common.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();

        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);

        SysOperationLog log = new SysOperationLog();
        log.setOperation(StringUtils.hasText(annotation.value()) ? annotation.value() : "未知操作");
        log.setSubsystemCode(StringUtils.hasText(annotation.subsystem()) ? annotation.subsystem() : "SYSTEM");
        // Bug1 修复: 确保level永远不为null
        log.setLevel(StringUtils.hasText(annotation.level()) ? annotation.level() : "INFO");
        log.setStatus(1); // 默认成功

        // 获取请求信息和用户
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                log.setMethod(request.getMethod() + " " + request.getRequestURI());
                log.setIp(getClientIp(request));

                String bearerToken = request.getHeader("Authorization");
                if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                    String token = bearerToken.substring(7);
                    try {
                        log.setUserId(jwtUtil.getUserIdFromToken(token));
                        log.setUsername(jwtUtil.getUsernameFromToken(token));
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}

        Object result = null;
        try {
            result = point.proceed();
            log.setStatus(1);
        } catch (Throwable e) {
            log.setStatus(0);
            log.setLevel("ERROR");
            String msg = e.getMessage();
            log.setErrorMsg(msg != null ? msg.substring(0, Math.min(msg.length(), 500)) : "Unknown error");
            throw e;
        } finally {
            log.setDuration(System.currentTimeMillis() - startTime);
            if (logWriter != null) { try { logWriter.writeLog(log); } catch (Exception ignored) {} }
        }
        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getRemoteAddr();
        if (ip != null && ip.contains(","))
            ip = ip.split(",")[0].trim();
        return ip;
    }
}
