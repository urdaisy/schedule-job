package com.schedule.job.security.proxy.handler;

import com.schedule.job.security.aop.AdviceDefinition;
import com.schedule.job.security.aop.AspectDefinition;
import com.schedule.job.security.reflect.JoinPoint;
import com.schedule.job.security.reflect.ProceedingJoinPoint;
import com.schedule.job.security.reflect.impl.MethodInvocationJoinPoint;
import com.schedule.job.security.util.AdviceType;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 参考来源于官方org.springframework.aop.framework中JdkDynamicAopProxy
 */
public class JdkInvocationHandler implements InvocationHandler {
    private Object target;
    private List<AspectDefinition> aspectDefinitions;
    public JdkInvocationHandler(Object target, List<AspectDefinition> aspectDefinitions) {
        this.target = target;
        this.aspectDefinitions = aspectDefinitions;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 1. 如果没有切面，直接执行目标方法
        if (aspectDefinitions == null || aspectDefinitions.isEmpty()) {
            return method.invoke(target, args);
        }
        
        // 2. 查找匹配的切面。如果没有切面，直接执行目标方法
        List<AspectDefinition> matchedAspects = getAspectDefinitions(method);
        if (matchedAspects.isEmpty()) {
            return method.invoke(target, args);
        }
        
        // 3. 创建连接点
        ProceedingJoinPoint proceedingJoinPoint = new MethodInvocationJoinPoint(target, method, args);
        
        // 4. 执行 Before 通知
        executeBeforeAdvices(matchedAspects, proceedingJoinPoint);
        
        // 5. 执行 Around 通知（嵌套执行）
        Object result = executeAroundAdvices(matchedAspects, proceedingJoinPoint);
        
        // 6. 执行 After 通知
        executeAfterAdvices(matchedAspects, proceedingJoinPoint, result);
        
        return result;
    }

    private List<AspectDefinition> getAspectDefinitions(Method method) {
        if (aspectDefinitions == null || aspectDefinitions.isEmpty()) {
            return Collections.emptyList();
        }
        List<AspectDefinition> matched = new ArrayList<>();
        for (AspectDefinition aspectDefinition : aspectDefinitions) {
            // matches 方法需要传入 method 和 targetClass
            if (aspectDefinition.matches(method, target.getClass())) {
                matched.add(aspectDefinition);
            }
        }
        return matched;
    }

    private void executeBeforeAdvices(List<AspectDefinition> aspects, JoinPoint joinPoint) {
        for (AspectDefinition aspect : aspects) {
            for (AdviceDefinition advice : aspect.getAdvices()) {
                if (advice.getAdviceType() == AdviceType.BEFORE) {
                    try {
                        advice.getAdviceMethod().invoke(aspect.getAspectInstance(), joinPoint);
                    } catch (Exception e) {
                        throw new RuntimeException("执行 Before 通知失败", e);
                    }
                }
            }
        }
    }

    private Object executeAroundAdvices(List<AspectDefinition> aspects, ProceedingJoinPoint joinPoint) throws Throwable {
        if (aspects.isEmpty()) {
            return joinPoint.proceed();
        }

        // 收集所有 Around 通知
        List<AdviceDefinition> aroundAdvices = new ArrayList<>();
        for (AspectDefinition aspect : aspects) {
            for (AdviceDefinition advice : aspect.getAdvices()) {
                if (advice.getAdviceType() == AdviceType.AROUND) {
                    aroundAdvices.add(advice);
                }
            }
        }

        // 嵌套执行 Around 通知
        return executeAroundRecursive(aroundAdvices, 0, joinPoint, aspects);
    }

    private Object executeAroundRecursive(List<AdviceDefinition> advices, int index, ProceedingJoinPoint joinPoint, List<AspectDefinition> aspects) throws Throwable {
        if (index >= advices.size()) {
            return joinPoint.proceed();
        }

        // 找到当前通知对应的定义切面AspectDefinition
        AdviceDefinition current = advices.get(index);
        AspectDefinition aspect = null;
        for (AspectDefinition a : aspects) {
            if (a.getAdvices() != null && a.getAdvices().contains(current)) {
                aspect = a;
                break;
            }
        }
        
        if (aspect == null) {
            return joinPoint.proceed();
        }
        
        // 创建新的 ProceedingJoinPoint，指向下一个 Around 通知
        ProceedingJoinPoint nextJoinPoint = new ProceedingJoinPoint() {
            @Override
            public Object proceed() throws Throwable {
                return executeAroundRecursive(advices, index + 1, joinPoint, aspects);
            }
            
            @Override
            public Object proceed(Object[] args) throws Throwable {
                return executeAroundRecursive(advices, index + 1, joinPoint, aspects);
            }
            
            @Override
            public Object getTarget() {
                return joinPoint.getTarget();
            }
            
            @Override
            public Method getMethod() {
                return joinPoint.getMethod();
            }
            
            @Override
            public Object[] getArgs() {
                return joinPoint.getArgs();
            }
        };
        
        try {
            return current.getAdviceMethod().invoke(aspect.getAspectInstance(), nextJoinPoint);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private Object executeAfterAdvices(List<AspectDefinition> aspects, JoinPoint joinPoint, Object result) {
        for (AspectDefinition aspect : aspects) {
            if (aspect.getAdvices() == null) {
                continue;
            }
            for (AdviceDefinition advice : aspect.getAdvices()) {
                if (advice.getAdviceType() == AdviceType.AFTER) {
                    try {
                        advice.getAdviceMethod().invoke(aspect.getAspectInstance(), joinPoint);
                    } catch (Exception e) {
                        throw new RuntimeException("执行 After 通知失败", e);
                    }
                }
            }
        }
        return result;
    }
}
