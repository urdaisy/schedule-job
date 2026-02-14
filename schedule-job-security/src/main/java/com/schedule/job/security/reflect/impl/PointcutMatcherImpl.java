package com.schedule.job.security.reflect.impl;

import com.schedule.job.common.exception.BusinessException;
import com.schedule.job.common.util.MatcherUtil;
import com.schedule.job.security.aop.ExecutionPointcutNode;
import com.schedule.job.security.reflect.PointcutMatcher;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

import static com.schedule.job.common.enums.BusinessExceptionCode.REFLECT_PARAM_ERROR;

@Slf4j
public class PointcutMatcherImpl implements PointcutMatcher {
    // 将字符串转换为正则表达式
    private static final String REGEXP_PATTERN = "\\s+";

    /**
     * 解析表达式,
     * 标准格式：execution(修饰符? 返回类型 类名.方法名(参数))
     * 例如：execution(* com.schedule.job.admin.controller.*.*(..))
     *
     * @param expression
     * @return
     */
    @Override
    public ExecutionPointcutNode parse(String expression) {
        if (expression == null || expression.isEmpty()) {
            log.error("切点表达式异常：{}", expression);
            throw new BusinessException(REFLECT_PARAM_ERROR);
        }
        int length = expression.length();
        // 去除execute()，获取里面的路径：返回类型 类名 方法名(方法参数)
        if (!expression.startsWith("execution(") || !expression.endsWith(")")) {
            log.error("切点表达式异常：{}", expression);
            throw new BusinessException(REFLECT_PARAM_ERROR);
        }
        String expressionsExclude = expression.substring("execution(".length(), length-1);
        if (StringUtil.isEmpty(expressionsExclude)) {
            log.error("未指定执行切点的方法:{}, 去掉execute后方法为空", expression);
            throw new BusinessException(REFLECT_PARAM_ERROR);
        }
        List<String> expressionsExcludeList = Arrays.asList(expressionsExclude.split(" "));
        String returnTypePattern = expressionsExcludeList.get(0);
        // 按照“(“拆分
        int leftMethodName = expressionsExcludeList.get(1).indexOf("(");
        int rightMethodName = expressionsExcludeList.get(2).indexOf(")");
        if (leftMethodName == -1 || rightMethodName == -1 || rightMethodName <= leftMethodName) {
            log.error("缺少方法参数");
            throw new BusinessException(REFLECT_PARAM_ERROR);
        }
        String methodNamePattern = expressionsExcludeList.get(1).substring(leftMethodName-1, leftMethodName);
        String paramPattern = expressionsExcludeList.get(1).substring(leftMethodName + 1, rightMethodName);
        String classNamePattern = expressionsExcludeList.get(1).substring(0, leftMethodName - 2);
        return new ExecutionPointcutNode(returnTypePattern, classNamePattern, methodNamePattern, paramPattern);
    }

    /**
     * 判断方法是否匹配切点
     * @param method
     * @param targetClass
     * @param expression
     *
     * @return
     */
    @Override
    public boolean matches(Method method, Class<?> targetClass, String expression) {
        ExecutionPointcutNode executionPointcutNode = parse(expression);
        // 获取类名
        String targetClassName = targetClass.getName();
        // 获取方法名
        String targetMethodName = method.getName();
        // 获取返回值
        String targetReturnType = method.getReturnType().getName();
        // 获取参数
        Parameter[] targetParameters = method.getParameters();
        return MatcherUtil.isMatchClassName(executionPointcutNode.getClassNamePattern(), targetClassName)
                && MatcherUtil.isMatchMethodName(executionPointcutNode.getMethodNamePattern(), targetMethodName)
                && MatcherUtil.isMatchParamName(executionPointcutNode.getParamPattern(), targetParameters)
                && MatcherUtil.isMatchReturnType(executionPointcutNode.getReturnTypePattern(), targetReturnType);
    }
}
