package com.schedule.job.security.reflect;

import java.lang.reflect.Method;

/**
 * 定义连接点：目标实例、方法、参数
 */
public interface JoinPoint {
    Object getTarget();           // 目标对象
    Method getMethod();           // 目标方法
    Object[] getArgs();          // 方法参数
}