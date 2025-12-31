package com.schedule.job.admin.job;

import lombok.Data;
import org.quartz.JobDataMap;

@Data
public class JobExecutionContext {
    JobDataMap jobDataMap;

    public JobDataMap getMergedJobDataMap() {
        return jobDataMap;
    }
}
