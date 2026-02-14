package com.schedule.job.common.enums;

import lombok.Getter;

@Getter
public enum BusinessExceptionCode {
    // 基础校验任务
    JOB_NOT_FOUND(0000, "job not found"),
    JOB_PARAM_FAILED(0001, "job param error"),
    JOB_TRIGGER_NOT_FOUND(0010, "job trigger not found"),
    // CRUD失败任务
    JOB_INSERT_FAILED(1000, "order insert failed"),
    JOB_DELETE_FAILED(1001, "order delete failed"),
    JOB_UPDATE_FAILED(1002, "order update failed"),
    JOB_SEARCH_FAILED(1003, "order search failed"),
    JOB_PAUSE_FAILED(1004, "order pause failed"),
    JOB_RETRY_FAILED(1005, "job retry failed"),
    JOB_LOAD_FAILED(1006, "job load failed"),
    // 用户校验异常
    USER_PARAM_ERROR(2000, "user param error"),
    USER_NOT_FOUND(2001, "user not found"),
    USER_UPDATE_FAILED(2002, "user update failed"),
    ROLE_NOT_FOUND(2003, "role not found"),
    PERMISSION_NOT_FOUND(2004, "permission not found"),
    USER_SESSION_NOT_FOUND(2005, "user session not found"),
    USER_SESSION_UPDATE_FAILED(2006, "user session update failed"),
    // 反射校验异常
    REFLECT_PARAM_ERROR(30000, "reflect param error"),

    ;
    private int code;
    private String message;

    BusinessExceptionCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
