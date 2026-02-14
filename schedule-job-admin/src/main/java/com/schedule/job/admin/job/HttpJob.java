package com.schedule.job.admin.job;

import com.schedule.job.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static com.schedule.job.common.enums.BusinessExceptionCode.JOB_RETRY_FAILED;

/**
 * HTTP请求任务
 * 用于执行HTTP/HTTPS请求任务
 * 
 * 参数格式：url|method|body(可选)
 * 示例：
 * - GET请求：https://api.example.com/endpoint|GET|
 * - POST请求：https://api.example.com/endpoint|POST|{"key":"value"}
 *
 */
@Slf4j
public class HttpJob extends BaseJob {
    @Override
    protected void executeInternal(String jobName, String jobParam) throws Exception {
        log.info("执行HTTP任务：{}，参数：{}", jobName, jobParam);
        
        // 解析参数
        String[] params = jobParam.split("\\|");
        if (params.length < 2) {
            throw new IllegalArgumentException("HTTP任务参数格式错误，需要：url|method|body(可选)");
        }
        
        String urlStr = params[0].trim();
        String method = params[1].trim().toUpperCase();
        String body = params.length > 2 ? params[2].trim() : null;
        
        // 执行HTTP请求
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        connection.setDoInput(true);
        
        // 设置请求头
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "ScheduleJob/1.0");
        
        // 如果有请求体，写入
        if (body != null && !body.isEmpty() && ("POST".equals(method) || "PUT".equals(method))) {
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        }
        
        // 读取响应
        int responseCode = connection.getResponseCode();
        StringBuilder response = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    responseCode >= 200 && responseCode < 300 
                        ? connection.getInputStream() 
                        : connection.getErrorStream(),
                    StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        
        String result = String.format("HTTP %d: %s", responseCode, response.toString());
        
        // 判断是否成功（2xx状态码）
        if (responseCode < 200 || responseCode >= 300) {
            throw new Exception("HTTP请求失败：" + result);
        }
        
        log.info("HTTP任务执行成功：{}", result);
    }

    @Override
    protected void scheduleRetryJob(Scheduler scheduler, String jobName, String jobGroup, 
                                   int retryInterval, int nextRetryCount, int maxRetryCount,
                                   Long jobId, String jobParam) throws Exception {
        if (retryInterval <= 0) {
            retryInterval = 60;
            log.warn("重试间隔无效，使用默认值60秒");
        }
        
        String retryTriggerKey = jobName + "_retry_" + System.currentTimeMillis();
        TriggerKey triggerKey = TriggerKey.triggerKey(retryTriggerKey, jobGroup + "_retry");
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        
        if (!scheduler.checkExists(jobKey)) {
            log.error("原始任务不存在，无法创建重试任务：jobName={}, jobGroup={}", jobName, jobGroup);
            throw new BusinessException(JOB_RETRY_FAILED, "原始任务不存在，无法创建重试任务");
        }
        
        // 获取原始JobDetail，以便复制JobDataMap
        JobDetail originalJobDetail = scheduler.getJobDetail(jobKey);
        if (originalJobDetail == null) {
            log.error("无法获取原始任务详情：jobName={}, jobGroup={}", jobName, jobGroup);
            throw new BusinessException(JOB_RETRY_FAILED, "无法获取原始任务详情");
        }
        
        // 创建新的JobDataMap，包含重试信息
        JobDataMap retryDataMap = new JobDataMap(originalJobDetail.getJobDataMap());
        retryDataMap.put("currentRetryCount", nextRetryCount);
        retryDataMap.put("maxRetryCount", maxRetryCount);
        retryDataMap.put("retryInterval", retryInterval);
        
        Date startTime = new Date(System.currentTimeMillis() + retryInterval * 1000L);
        
        SimpleTrigger retryTrigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .forJob(jobKey)
                .usingJobData(retryDataMap)
                .startAt(startTime)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withRepeatCount(0)
                        .withIntervalInMilliseconds(0))
                .build();
        
        scheduler.scheduleJob(retryTrigger);
        
        log.info("创建HTTP任务重试成功：jobName={}, jobGroup={}, 第{}次重试将在{}秒后执行", 
                jobName, jobGroup, nextRetryCount, retryInterval);
    }
}
