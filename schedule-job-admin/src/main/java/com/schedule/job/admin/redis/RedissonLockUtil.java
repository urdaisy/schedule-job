package com.schedule.job.admin.redis;

import com.schedule.job.admin.config.RedisConfig;
import com.schedule.job.common.constant.RedissonConstants;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RedissonLockUtil {

    @Autowired
    private RedisConfig redisConfig;

    private RLock getLock(String lockKey)  {
        String fullNameLock = RedissonConstants.LOCK_PREFIX + lockKey;
        RedissonClient redissonClient = redisConfig.createRedissonClient();
        log.debug("获取分布式锁：{}", fullNameLock);
        return redissonClient.getLock(fullNameLock);
    }

    /**
     * 尝试获取锁
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            log.error("获取锁异常", e);
            return false;
        }
    }

    /**
     * 释放锁
     */
    public void unlock(String lockKey) {
        try {
            String fullNameLock = RedissonConstants.LOCK_PREFIX + lockKey;
            RLock lock = getLock(fullNameLock);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (Exception e) {
            log.error("释放锁异常", e);
        }
    }
}