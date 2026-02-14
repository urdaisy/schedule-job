package com.schedule.job.security.proxy;

import com.schedule.job.common.enums.BusinessExceptionCode;
import com.schedule.job.common.exception.BusinessException;
import com.schedule.job.security.aop.AspectDefinition;
import com.schedule.job.security.proxy.handler.JdkInvocationHandler;
import com.schedule.job.security.proxy.interceptor.CglibMethodInterceptor;
import org.springframework.cglib.proxy.Enhancer;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Objects;

/**
 * 工厂模式
 */
public class ProxyFactory {

    /**
     * 创建代理对象
     * 有接口使用JDK动态代理，无接口使用CGLIB静态代理
     * 代理的目的：
     * @param target
     * @return
     */
    public static Object createProxyInstance(Object target, List<AspectDefinition> aspect) {
        if (Objects.isNull(target)) {
            throw new BusinessException(BusinessExceptionCode.REFLECT_PARAM_ERROR,
                    "Cannot create class proxy for TargetSource with null target class");
        }
        Class<?> targetClass = target.getClass();
        if (targetClass.getInterfaces().length > 0) {
            return createJdkProxy(target, aspect);
        } else {
            return createCglibProxy(target, aspect);
        }
    }

    private static Object createJdkProxy(Object target, List<AspectDefinition> aspects) {
        return Proxy.newProxyInstance(target.getClass().getClassLoader(),  target.getClass().getInterfaces(),
                new JdkInvocationHandler(target, aspects));
    }

    private static Object createCglibProxy(Object target, List<AspectDefinition> aspects) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(target.getClass());
        enhancer.setCallback(new CglibMethodInterceptor(target, aspects));
        return enhancer.create();
    }
}
