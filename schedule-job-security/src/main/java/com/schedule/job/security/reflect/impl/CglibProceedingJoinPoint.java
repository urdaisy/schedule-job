package com.schedule.job.security.reflect.impl;

import com.schedule.job.security.reflect.ProceedingJoinPoint;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * CGLIB 代理专用的 ProceedingJoinPoint 实现
 */
public class CglibProceedingJoinPoint implements ProceedingJoinPoint {
    private final Object target;
    private final Method method;
    private final Object[] args;
    private final MethodProxy methodProxy;
    private final Object proxy;

    public CglibProceedingJoinPoint(Object target, Method method, Object[] args, MethodProxy methodProxy, Object proxy) {
        this.target = target;
        this.method = method;
        this.args = args;
        this.methodProxy = methodProxy;
        this.proxy = proxy;
    }

    @Override
    public Object proceed() throws Throwable {
        return methodProxy.invokeSuper(proxy, args);
    }

    @Override
    public Object proceed(Object[] args) throws Throwable {
        // 使用传入的新参数执行目标方法
        return methodProxy.invokeSuper(proxy, args != null ? args : this.args);
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

    // Getter 方法，用于在递归调用中创建新的实例
    public MethodProxy getMethodProxy() {
        return methodProxy;
    }

    public Object getProxy() {
        return proxy;
    }
}
