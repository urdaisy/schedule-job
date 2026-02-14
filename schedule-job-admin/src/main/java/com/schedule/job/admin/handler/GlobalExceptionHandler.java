package com.schedule.job.admin.handler;

import com.schedule.job.common.exception.BusinessException;
import com.schedule.job.common.exception.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 全局异常处理器
 */
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.error(e.getMessage());
    }

    // 全局通用异常处理，简化控制器中的try-catch捕获异常的处理
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        return Result.error(e.getMessage());
    }
}
