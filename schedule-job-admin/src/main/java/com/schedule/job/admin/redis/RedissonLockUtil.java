package com.schedule.job.admin.redis;

import com.schedule.job.admin.config.RedisConfig;
import com.schedule.job.common.constant.RedissonConstants;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式锁工具类
 */
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

    /**
     * 多键有序加锁上下文，用于 {@link #tryLockOrdered} 成功后、在 finally 中调用 {@link #unlockOrdered} 释放。
     */
    public static class OrderedLockContext {
        private final List<String> lockedKeys;

        OrderedLockContext(List<String> lockedKeys) {
            this.lockedKeys = lockedKeys;
        }
    }

    /**
     * 对多个 key 按字典序依次加锁，避免不同线程以不同顺序加锁导致死锁。
     * 若某个 key 获取失败，会释放已获取的锁并返回 null，调用方需在 finally 中仅对非 null 的 context 调用 {@link #unlockOrdered}。
     *
     * @param lockKeys   待加锁的 key 列表，内部会先排序再按顺序加锁
     * @param waitTime  单把锁等待时间
     * @param leaseTime 单把锁持有时间（自动过期）
     * @param unit      时间单位
     * @return 成功时返回上下文，用于 finally 中 unlockOrdered；失败返回 null
     */
    public OrderedLockContext tryLockOrdered(List<String> lockKeys, long waitTime, long leaseTime, TimeUnit unit) {
        if (lockKeys == null || lockKeys.isEmpty()) {
            return new OrderedLockContext(Collections.emptyList());
        }
        List<String> sorted = new ArrayList<>(lockKeys);
        Collections.sort(sorted);

        List<String> acquired = new ArrayList<>(sorted.size());
        try {
            for (String key : sorted) {
                if (!tryLock(key, waitTime, leaseTime, unit)) {
                    releaseAcquiredInReverse(acquired);
                    return null;
                }
                acquired.add(key);
            }
            return new OrderedLockContext(acquired);
        } catch (Exception e) {
            releaseAcquiredInReverse(acquired);
            log.error("有序加锁异常", e);
            return null;
        }
    }

    private void releaseAcquiredInReverse(List<String> acquired) {
        for (int i = acquired.size() - 1; i >= 0; i--) {
            unlock(acquired.get(i));
        }
    }

    /**
     * 释放 {@link #tryLockOrdered} 获取的有序锁，按加锁的逆序释放。
     *
     * @param context tryLockOrdered 返回的上下文，可为 null（内部会忽略）
     */
    public void unlockOrdered(OrderedLockContext context) {
        if (context == null || context.lockedKeys == null) {
            return;
        }
        for (int i = context.lockedKeys.size() - 1; i >= 0; i--) {
            unlock(context.lockedKeys.get(i));
        }
    }
}