package com.schedule.job.security.proxy;

import com.schedule.job.common.enums.BusinessExceptionCode;
import com.schedule.job.common.exception.BusinessException;
import com.schedule.job.security.aop.AspectDefinition;
import com.schedule.job.security.proxy.handler.JdkInvocationHandler;
import com.schedule.job.security.proxy.interceptor.CglibMethodInterceptor;
import org.springframework.cglib.proxy.Enhancer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
     *
     * @param target 原始 Bean（Spring 已注入依赖）
     * @param aspect  切面定义
     * @return 代理实例。CGLIB 时会将 target 上已注入的字段复制到代理，使 invokeSuper(proxy, args) 时 this 的依赖可用
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
        Object proxy = enhancer.create();
        // 将 target 上已被 Spring 注入的字段复制到代理实例，使方法在 proxy 上执行时 this.xxx 不为 null
        copyInjectedFieldsToProxy(target, proxy);
        return proxy;
    }

    /**
     * 将 target 上已注入的实例字段复制到 proxy（target 与 proxy 同属 target 类或其子类）。
     * 这样 CGLIB 使用 invokeSuper(proxy, args) 时，方法内的 this 是 proxy，其依赖已被填充。
     */
    private static void copyInjectedFieldsToProxy(Object target, Object proxy) {
        Class<?> sourceClazz = target.getClass();
        while (sourceClazz != null && sourceClazz != Object.class) {
            for (Field field : sourceClazz.getDeclaredFields()) {
                // field.getModifiers()获取成员变量的修饰符
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object targetObject = field.get(target);
                    field.set(proxy, targetObject);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("复制注入字段到代理失败: " + sourceClazz.getName() + "." + field.getName(), e);
                }
            }
            sourceClazz = sourceClazz.getSuperclass();
        }
    }
}
