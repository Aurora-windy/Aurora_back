package com.aurora.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解（Phase 1 Task 1.9 配 AOP 实现）
 *
 * <p>用法：在 Controller 方法上加 {@code @Log("新增用户")}，
 * AOP 自动记录到 sys_log 表。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {

    /**
     * 操作描述
     */
    String value() default "";
}
