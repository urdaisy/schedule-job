package com.schedule.job.security.aop;

import com.schedule.job.security.proxy.ProxyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 创建代理对象
 */
@Component
@Slf4j
public class AspectBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {
    private AspectScanner aspectScanner;
    private List<AspectDefinition> aspects;
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        // 初始化 AspectScanner，并保存到spring上下文中
        this.aspectScanner = new AspectScanner();
        this.aspectScanner.setApplicationContext(applicationContext);
        // 扫描 @Aspect 注解的类
        this.aspects = aspectScanner.scanAspects("com.schedule.job");
        log.info("Scanned aspects: {}", aspects.size());
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 1. 扫描所有@Aspect 注解的类
        List<AspectDefinition> matchedAspects = findMatchedAspects(bean);
        if (matchedAspects.isEmpty()) {
            return bean;
        }

        // 2. 创建代理对象
        return ProxyFactory.createProxyInstance(bean, matchedAspects);
    }

    private List<AspectDefinition> findMatchedAspects(Object bean) {
        // 如果切面列表为空，直接返回空列表
        if (aspects == null || aspects.isEmpty()) {
            return Collections.emptyList();
        }

        // 使用目标类：若 bean 已是代理（如 CGLIB），getDeclaredMethods 只含代理类方法，需用实际目标类
        Class<?> beanClass = AopUtils.getTargetClass(bean);
        List<AspectDefinition> matched = new ArrayList<>();

        // 检查目标类及其父类的所有声明方法（含 @RequirePermission 等）
        for (Class<?> clazz = beanClass; clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                for (AspectDefinition aspect : aspects) {
                    if (aspect.matches(method, beanClass)) {
                        if (!matched.contains(aspect)) {
                            matched.add(aspect);
                        }
                    }
                }
            }
        }

        return matched;
    }
}