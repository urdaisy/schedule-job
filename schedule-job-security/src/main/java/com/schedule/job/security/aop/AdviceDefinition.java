package com.schedule.job.security.aop;

import com.schedule.job.security.util.AdviceType;
import lombok.Data;

import java.lang.reflect.Method;

/**
 * 定义通知：通知类型、通知方法、切点表达式、优先级
 */
@Data
public class AdviceDefinition {
    private AdviceType adviceType;
    private Method adviceMethod;
    private String pointcutExpression;
    private int order;

    public AdviceDefinition(AdviceType adviceType, Method adviceMethod, String pointcutExpression, int order) {
        this.adviceType = adviceType;
        this.adviceMethod = adviceMethod;
        this.pointcutExpression = pointcutExpression;
        this.order = order;
    }
}
