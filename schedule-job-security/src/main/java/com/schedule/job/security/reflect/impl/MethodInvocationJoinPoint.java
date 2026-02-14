package com.schedule.job.security.reflect.impl;

import com.schedule.job.security.reflect.ProceedingJoinPoint;

import java.lang.reflect.Method;

public class MethodInvocationJoinPoint implements ProceedingJoinPoint {
    private final Object target;
    private final Method method;
    private final Object[] args;
    public MethodInvocationJoinPoint(Object target, Method method, Object[] args) {
        this.target = target;
        this.method = method;
        this.args = args;
    }

    @Override
    public Object proceed() throws Throwable {
        return method.invoke(target, args);
    }
    @Override
    public Object proceed(Object[] args) throws Throwable {
        // 使用新的参数执行目标方法
        return method.invoke(target, args);
    }
    
    @Override
    public Object getTarget() {
        return target;
    }
    
    @Override
    public Method getMethod() {
        return method;
    }
    
    @Override
    public Object[] getArgs() {
        return args != null ? args : new Object[0];
    }
}
