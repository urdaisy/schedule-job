package com.schedule.job.common.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Logger;

public class TypeConvertorUtil {
    private static final Logger log = Logger.getLogger(TypeConvertorUtil.class.getName());
    public static <T> T convert(Class<T> targetTypeClazz, Object sourceValue) {
        if (sourceValue == null) {
            return null;
        }
        // 类型已经匹配，直接强转
        if (targetTypeClazz.isInstance(sourceValue)) {
            return (T) sourceValue;
        }

        // 获取原始值类型
        Class<?> sourceType = sourceValue.getClass();
        Object targetValue = null;
        if (targetTypeClazz == Long.class || targetTypeClazz == long.class) {
            if (sourceValue instanceof BigInteger) {
                targetValue = ((BigInteger) sourceValue).longValue();
            }
            if (sourceValue instanceof BigDecimal) {
                targetValue = ((BigDecimal) sourceValue).doubleValue();
            }
            if (sourceValue instanceof Integer) {
                targetValue = ((Integer) sourceValue).intValue();
            }
        }
        else if (targetTypeClazz == Integer.class || targetTypeClazz == int.class) {
            if (sourceValue instanceof BigInteger) {
                targetValue = ((BigInteger) sourceValue).intValue();
            } else if (sourceValue instanceof Long) {
                targetValue = ((Long) sourceValue).intValue();
            } else if (sourceValue instanceof Boolean) {
                if (!((Boolean) sourceValue)) {
                    targetValue = 0;
                } else {
                    targetValue = 1;
                }
            }
        }
        else if (targetTypeClazz == Date.class) {
            if (sourceValue instanceof Timestamp) {
                targetValue = new Date(((Timestamp) sourceValue).getTime());
            }
        }
        else if (targetTypeClazz == String.class) {
            targetValue = sourceValue.toString();
        }

        if (targetValue != null) {
            return (T) targetValue;
        }

        // 不支持的转换类型，抛异常并打印日志
        String errorMsg = String.format("不支持的类型转换：源类型[%s] 目标类型[%s] 源值[%s]", sourceType.getName(), targetTypeClazz.getName(), sourceValue);
        log.severe(errorMsg);
        throw new RuntimeException(errorMsg);
    }
}
