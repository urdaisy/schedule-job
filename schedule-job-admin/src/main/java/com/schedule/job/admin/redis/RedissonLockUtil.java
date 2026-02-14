package com.schedule.job.admin.redis;

import com.schedule.job.admin.config.RedisConfig;
import com.schedule.job.common.constant.RedissonConstants;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RedissonLockUtil {

    @Autowired
    private RedisConfig redisConfig;
    
    private RedissonClient redissonClient;

    /**
     * 初始化 RedissonClient（只创建一次，避免重复创建）
     */
    @PostConstruct
    public void init() {
        this.redissonClient = redisConfig.createRedissonClient();
        log.info("RedissonClient 初始化完成");
    }

    /**
     * 获取锁对象
     * @param lockKey 锁的key（不包含前缀）
     * @return RLock 锁对象
     */
    private RLock getLock(String lockKey) {
        String fullNameLock = RedissonConstants.LOCK_PREFIX + lockKey;
        if (redissonClient == null) {
            log.warn("RedissonClient 未初始化，尝试重新初始化");
            init();
        }
        log.debug("获取分布式锁：{}", fullNameLock);
        return redissonClient.getLock(fullNameLock);
    }

    /**
     * 尝试获取锁
     * @param lockKey 锁的key（不包含前缀）
     * @param waitTime 等待获取锁的最大时间
     * @param leaseTime 锁的持有时间（自动释放时间）
     * @param unit 时间单位
     * @return true 如果成功获取锁，false 否则
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, unit);
            if (acquired) {
                log.debug("成功获取锁：{}", RedissonConstants.LOCK_PREFIX + lockKey);
            } else {
                log.warn("获取锁失败（超时或已被占用）：{}，等待时间：{} {}", 
                        RedissonConstants.LOCK_PREFIX + lockKey, waitTime, unit);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取锁被中断：{}", RedissonConstants.LOCK_PREFIX + lockKey, e);
            return false;
        } catch (Exception e) {
            log.error("获取锁异常：{}", RedissonConstants.LOCK_PREFIX + lockKey, e);
            return false;
        }
    }

    /**
     * 释放锁
     * @param lockKey 锁的key（不包含前缀）
     */
    public void unlock(String lockKey) {
        try {
            RLock lock = getLock(lockKey);
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("成功释放锁：{}", RedissonConstants.LOCK_PREFIX + lockKey);
            } else {
                log.warn("释放锁失败：锁不存在或不是当前线程持有，lockKey={}", 
                        RedissonConstants.LOCK_PREFIX + lockKey);
            }
        } catch (Exception e) {
            log.error("释放锁异常：{}", RedissonConstants.LOCK_PREFIX + lockKey, e);
        }
    }
}