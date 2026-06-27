package com.portal.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解 - 标记在需要记录日志的Controller方法上
 * 仅记录关键操作：登录、用户管理、权限变更、订单操作等
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    /** 操作描述 */
    String value() default "";

    /** 所属子系统编码 */
    String subsystem() default "";

    /** 日志级别，默认INFO */
    String level() default "INFO";
}
