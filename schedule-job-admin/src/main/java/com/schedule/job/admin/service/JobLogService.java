package com.schedule.job.admin.service;

import com.schedule.job.admin.entity.JobLogEntity;
import com.schedule.job.admin.job.JobLog;
import com.schedule.job.admin.repository.JobLogRepository;
import com.schedule.job.admin.repository.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Service
public class JobLogService {
    @Autowired
    private JobLogRepository jobLogRepository;

    public PageInfo<JobLog> findAllJobLog(Integer pageNum, Integer pageSize) {
        return jobLogRepository.queryJobLogByPage(pageNum, pageSize);
    }

    /**
     * 根据ID查询日志详情
     *
     * @param id 日志ID
     * @return 日志对象
     */
    public JobLog findById(Long id) throws SQLException {
        JobLogEntity jobLogEntity = jobLogRepository.findById(id);
        if (jobLogEntity == null || jobLogEntity.getDeleted() == 1) {
            return null;
        }
        return com.schedule.job.admin.entity.EntityConvert.convertToLogDomain(jobLogEntity);
    }

    /**
     * 根据ID删除日志（逻辑删除）
     *
     * @param id 日志ID
     * @return 是否删除成功
     */
    public boolean deleteById(Long id) throws SQLException {
        return jobLogRepository.deleteJobLog(id);
    }
}