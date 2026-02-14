package com.schedule.job.security.aop;

import lombok.Data;

import java.util.Arrays;
import java.util.List;

/**
 * 用于构建切点表达式
 */
@Data
public class ExecutionPointcutNode {
    // 返回类型
    public String returnTypePattern;
    // 类名
    public String classNamePattern;
    // 方法名
    public String methodNamePattern;
    // 参数列表
    public List<String> paramPattern;

    public ExecutionPointcutNode(String returnTypePattern, String classNamePattern, String methodNamePattern, String paramPattern) {
        this.returnTypePattern = returnTypePattern;
        this.classNamePattern = classNamePattern;
        this.methodNamePattern = methodNamePattern;
        if ("..".equals(paramPattern)) {
            this.paramPattern = Arrays.asList(paramPattern);
        } else {
            this.paramPattern = Arrays.asList(paramPattern.split(","));
        }
    }
}
