package com.schedule.job.security.reflect;

import com.schedule.job.security.aop.ExecutionPointcutNode;

import java.lang.reflect.Method;

public interface PointcutMatcher {
    public ExecutionPointcutNode parse(String expression);
    public boolean matches(Method method, Class<?> targetClass, String expression);
}
