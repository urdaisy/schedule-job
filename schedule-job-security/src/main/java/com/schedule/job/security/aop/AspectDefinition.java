package com.schedule.job.security.aop;

import com.schedule.job.security.reflect.PointcutMatcher;
import com.schedule.job.security.reflect.impl.PointcutMatcherImpl;
import lombok.Data;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 定义切面：包括切面实例、切点表达式、通知列表、优先级
 */
@Data
public class AspectDefinition {
    private Object aspectInstance;
    private String pointCutExpression;
    private List<AdviceDefinition> advices;
    private Integer sortOrder;

    /**
     * 判断切面实例是否匹配
     * @param method
     * @param targetClass
     * @return
     */
    public boolean matches(Method method, Class<?> targetClass) {
        // 1. 检查所有通知的切点表达式（因为一个切面可能有多个通知，每个通知可能有不同的切点）
        if (advices != null) {
            for (AdviceDefinition advice : advices) {
                String adviceExpression = advice.getPointcutExpression();
                if (adviceExpression != null && !adviceExpression.isEmpty()) {
                    if (matchesExpression(method, targetClass, adviceExpression)) {
                        return true;
                    }
                }
            }
        }
        
        // 2. 检查主切点表达式
        if (pointCutExpression != null && !pointCutExpression.isEmpty()) {
            if (matchesExpression(method, targetClass, this.pointCutExpression)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 判断方法是否匹配切点表达式（支持 execution 和 @annotation）
     */
    private boolean matchesExpression(Method method, Class<?> targetClass, String expression) {
        if (expression == null || expression.isEmpty()) {
            return false;
        }

        // 处理 @annotation 表达式
        if (expression.startsWith("@annotation(")) {
            return matchesAnnotationExpression(method, expression);
        }
        
        // 处理 execution 表达式
        if (expression.startsWith("execution(")) {
            PointcutMatcher matcher = new PointcutMatcherImpl();
            try {
                return matcher.matches(method, targetClass, expression);
            } catch (Exception e) {
                // 如果解析失败，返回 false
                return false;
            }
        }
        
        return false;
    }

    /**
     * 匹配 @annotation 表达式，如 @annotation(com.schedule.job.security.annotation.RequirePermission)
     */
    private boolean matchesAnnotationExpression(Method method, String expression) {
        try {
            // 提取注解类名：@annotation(com.schedule.job.security.annotation.RequirePermission)
            int start = expression.indexOf("@annotation(") + "@annotation(".length();
            int end = expression.lastIndexOf(")");
            if (start < 0 || end < 0 || end <= start) {
                return false;
            }

            String annotationClassName = expression.substring(start, end).trim();
            Class<?> annotationClass = Class.forName(annotationClassName);

            return method.isAnnotationPresent((Class<? extends Annotation>) annotationClass);
        } catch (Exception e) {
            return false;
        }
    }
}
