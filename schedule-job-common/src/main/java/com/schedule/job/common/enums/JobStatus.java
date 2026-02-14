package com.schedule.job.common.enums;

import lombok.Getter;

@Getter
public enum JobStatus {
    PAUSED(0),      // 暂停
    RUNNING(1),     // 运行中
    FAILED(2);      // 失败（达到最大重试次数）

    private int code;
    JobStatus(int code) {
        this.code = code;
    }
}
