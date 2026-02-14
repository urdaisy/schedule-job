package com.schedule.job.security.proxy.interceptor;

import com.schedule.job.security.aop.AdviceDefinition;
import com.schedule.job.security.aop.AspectDefinition;
import com.schedule.job.security.reflect.JoinPoint;
import com.schedule.job.security.reflect.ProceedingJoinPoint;
import com.schedule.job.security.reflect.impl.CglibProceedingJoinPoint;
import com.schedule.job.security.util.AdviceType;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CglibMethodInterceptor implements MethodInterceptor {
    private final Object target;
    private final List<AspectDefinition> aspectDefinitions;

    public CglibMethodInterceptor(Object target, List<AspectDefinition> aspectDefinitions) {
        this.target = target;
        this.aspectDefinitions = aspectDefinitions;
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        // 1. 如果没有切面，直接执行目标方法
        if (aspectDefinitions == null || aspectDefinitions.isEmpty()) {
            return proxy.invokeSuper(obj, args);
        }
        // 2. 查找匹配的切面，如果没有切面，直接执行目标方法
        List<AspectDefinition> matchedAspects = getAspectDefinitions(method);
        if (matchedAspects.isEmpty()) {
            return proxy.invokeSuper(obj, args);
        }
        
        // 3. 创建连接点
        ProceedingJoinPoint proceedingJoinPoint = new CglibProceedingJoinPoint(target, method, args, proxy, obj);
        
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
            if (aspectDefinition.matches(method, target.getClass())) {
                matched.add(aspectDefinition);
            }
        }
        return matched;
    }

    private void executeBeforeAdvices(List<AspectDefinition> aspects, JoinPoint joinPoint) {
        for (AspectDefinition aspect : aspects) {
            if (aspect.getAdvices() == null) {
                continue;
            }
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
            if (aspect.getAdvices() == null) {
                continue;
            }
            for (AdviceDefinition advice : aspect.getAdvices()) {
                if (advice.getAdviceType() == AdviceType.AROUND) {
                    aroundAdvices.add(advice);
                }
            }
        }

        // 嵌套执行 Around 通知
        return executeAroundRecursive(aroundAdvices, 0, joinPoint, aspects);
    }

    private Object executeAroundRecursive(List<AdviceDefinition> advices, int index, ProceedingJoinPoint originalJoinPoint, List<AspectDefinition> aspects) throws Throwable {
        // 如果所有 Around 通知都执行完了，调用原始 JoinPoint 的 proceed() 方法
        if (index >= advices.size()) {
            return originalJoinPoint.proceed();
        }

        AdviceDefinition current = advices.get(index);
        // 找到对应的 AspectDefinition
        AspectDefinition aspect = null;
        for (AspectDefinition a : aspects) {
            if (a.getAdvices() != null && a.getAdvices().contains(current)) {
                aspect = a;
                break;
            }
        }
        
        if (aspect == null) {
            return originalJoinPoint.proceed();
        }
        
        // 创建新的 ProceedingJoinPoint，指向下一个 Around 通知
        // 这个 JoinPoint 的 proceed() 方法会递归调用 executeAroundRecursive
        ProceedingJoinPoint nextJoinPoint = new ProceedingJoinPoint() {
            @Override
            public Object proceed() throws Throwable {
                // 递归调用下一个 Around 通知
                return executeAroundRecursive(advices, index + 1, originalJoinPoint, aspects);
            }
            
            @Override
            public Object proceed(Object[] args) throws Throwable {
                // 如果传入了新参数，需要创建一个新的 CglibProceedingJoinPoint
                if (originalJoinPoint instanceof CglibProceedingJoinPoint) {
                    CglibProceedingJoinPoint cglibJoinPoint = (CglibProceedingJoinPoint) originalJoinPoint;
                    // 创建新的 JoinPoint 使用新参数，确保使用 CGLIB 的 invokeSuper
                    ProceedingJoinPoint newJoinPoint = new CglibProceedingJoinPoint(
                        cglibJoinPoint.getTarget(),
                        cglibJoinPoint.getMethod(),
                        args,  // 使用传入的新参数
                        cglibJoinPoint.getMethodProxy(),  // 保持使用 CGLIB 的 MethodProxy
                        cglibJoinPoint.getProxy()  // 保持使用 CGLIB 的代理对象
                    );
                    return executeAroundRecursive(advices, index + 1, newJoinPoint, aspects);
                }
                // 如果不是 CglibProceedingJoinPoint，使用原始 JoinPoint（这种情况不应该发生）
                return executeAroundRecursive(advices, index + 1, originalJoinPoint, aspects);
            }
            
            @Override
            public Object getTarget() {
                return originalJoinPoint.getTarget();
            }
            
            @Override
            public Method getMethod() {
                return originalJoinPoint.getMethod();
            }
            
            @Override
            public Object[] getArgs() {
                return originalJoinPoint.getArgs();
            }
        };
        
        try {
            return current.getAdviceMethod().invoke(aspect.getAspectInstance(), nextJoinPoint);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private void executeAfterAdvices(List<AspectDefinition> aspects, JoinPoint joinPoint, Object result) {
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
    }
}
