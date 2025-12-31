package com.schedule.job.common.exception;

import com.schedule.job.common.enums.ResultCode;
import lombok.Data;

@Data
public class Result<T> {
    private final int code;
    private final String message;
    private T data;

    public Result(int code, String msg, T data) {
        this.code = code;
        this.message = msg;
        this.data = data;
    }

    // 返回成功响应（不带数据）
    public static Result<Void> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    // 返回成功响应（带数据）
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    // 返回成功响应（自定义消息）
    public static <T> Result<T> success(String msg) {
        return new Result<>(ResultCode.SUCCESS.getCode(), msg, null);
    }

    // 返回成功响应（自定义消息+带数据）
    public static <T> Result<T> success(String msg, T data) {
        return new Result<T>(ResultCode.SUCCESS.getCode(), msg, data);
    }

    // 返回错误响应（使用默认错误码和消息）
    public static Result<Void> error() {
        return new Result<>(ResultCode.ERROR.getCode(), ResultCode.ERROR.getMessage(), null);
    }

    // 返回错误响应（自定义消息）
    public static Result<Void> error(String message) {
        return new Result<>(ResultCode.ERROR.getCode(), message, null);
    }

    // 返回错误响应（指定状态码）
    public static Result<Void> error(ResultCode code) {
        return new Result<>(code.getCode(), code.getMessage(), null);
    }

    // 返回错误响应（指定状态码+消息）
    public static <T> Result<T> error(ResultCode code, String message) {
        return new Result<>(code.getCode(), message, null);
    }
}
