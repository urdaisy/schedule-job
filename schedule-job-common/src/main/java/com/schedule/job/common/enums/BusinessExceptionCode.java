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
    JOB_DELETE_FAILED(2000, "order delete failed"),
    JOB_UPDATE_FAILED(3000, "order update failed"),
    JOB_SEARCH_FAILED(4000, "order search failed"),
    JOB_PAUSE_FAILED(5000, "order pause failed");

    private int code;
    private String message;

    BusinessExceptionCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
