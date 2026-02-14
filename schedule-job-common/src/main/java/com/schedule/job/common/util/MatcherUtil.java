package com.schedule.job.common.util;

import java.lang.reflect.Parameter;
import java.util.List;

public class MatcherUtil {
    public static final String STAR = "*";
    public static final String REGEX_PERIOD = "\\.";
    public static final String ANY_PARAMETER = "..";

    public static boolean isMatchClassName(String className, String targetClassName) {
        String[] classNameArray = className.split(REGEX_PERIOD);
        String[] targetNameArray = targetClassName.split(REGEX_PERIOD);
        boolean match = true;
        int i = 0;
        while (i<classNameArray.length && i < targetNameArray.length) {
            if (classNameArray[i].equals(targetNameArray[i])) {
                continue;
            }
            if (classNameArray[i].equals(STAR)) {
                continue;
            } else {
                match = false;
            }
        }
        return match;
    }

    public static boolean isMatchMethodName(String methodName, String targetMethodName) {
        if (methodName.equals(STAR)) {
            return true;
        }
        return targetMethodName.equals(methodName);
    }

    public static boolean isMatchParamName(List<String> paramName, Parameter[] parameters) {
        if (paramName.size() == 1 && paramName.contains(ANY_PARAMETER)) {
            return true;
        }
        boolean match = true;
        for (int i = 0; i < parameters.length; i++) {
            if (paramName.get(i).equals(parameters[i].getType().getName())) {
                continue;
            } else {
                match = false;
            }
        }
        return match;
    }

    public static boolean isMatchReturnType(String returnType, String targetReturnType) {
        if (returnType.equals(STAR)) {
            return true;
        }
        return targetReturnType.equals(returnType);
    }
}
