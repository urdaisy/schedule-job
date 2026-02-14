package com.schedule.job.security.aop;

import com.schedule.job.security.proxy.ProxyFactory;
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

        Class<?> beanClass = bean.getClass();
        List<AspectDefinition> matched = new ArrayList<>();

        // 检查所有方法
        Method[] methods = beanClass.getDeclaredMethods();
        for (Method method : methods) {
            for (AspectDefinition aspect : aspects) {
                if (aspect.matches(method, beanClass)) {
                    if (!matched.contains(aspect)) {
                        matched.add(aspect);
                    }
                }
            }
        }

        return matched;
    }
}