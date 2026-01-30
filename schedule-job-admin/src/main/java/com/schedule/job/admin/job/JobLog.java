package com.schedule.job.admin.job;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobLog {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 任务ID
     */
    private Long jobId;

    /**
     * 任务触发时间
     */
    private LocalDateTime triggerTime;

    /**
     * 任务开始执行时间
     */
    private LocalDateTime executeTime;

    /**
     * 执行耗时(毫秒)
     */
    private Long duration;

    /**
     * 状态：0-成功，1-失败
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 日志创建时间
     */
    private LocalDateTime createTime;

    /**
     * 日志更新时间逻辑删除
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    private Integer deleted;
}