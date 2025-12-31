package com.schedule.job.common.exception;

import com.schedule.job.common.enums.BusinessExceptionCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private int code;

    // 使用代码内部枚举的错误码和消息
    public BusinessException(BusinessExceptionCode code) {
        super(code.getMessage());
        this.code = code.getCode();
    }

    // 使用代码内部枚举的错误码和自定义消息
    public BusinessException(BusinessExceptionCode code, String message) {
        super(message);
        this.code = code.getCode();
    }
}
