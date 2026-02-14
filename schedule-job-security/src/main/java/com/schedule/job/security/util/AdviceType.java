package com.schedule.job.security.util;

public enum AdviceType {
    AROUND("Around"),
    BEFORE("Before"),
    AFTER("After"),
    ;

    private String adviceType;
    private AdviceType(String adviceType) {
        this.adviceType = adviceType;
    }

    public String getAdviceType() {
        return adviceType;
    }
}
