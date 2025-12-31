package com.schedule.job.common.enums;

import lombok.Getter;

@Getter
public enum JobStatus {
    PAUSED(0),
    RUNNING(1);

    private int code;
    JobStatus(int code) {
        this.code = code;
    }
}
