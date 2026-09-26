package com.bitejiuyeke.bitecommonredis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁
 */
@Slf4j
@RequiredArgsConstructor
public class RedissonLockService {
    /**
     * redis操作客户端
     */
    private final RedissonClient redissonClient;

    /**
     * 获取锁
     *
     * @param lockKey        锁的key，唯一标识，建议模块名+唯一键
     * @param expire         超时时间，单位毫秒，传入-1自动续期
     * @return 获取到的RLock实例，为null则获取失败
     */
    public RLock acquire(String lockKey, long expire) {
        try {
            final RLock lockInstance = redissonClient.getLock(lockKey);

            // 注意：如果tryLock指定了leaseTime>0就不会续期。参考 RedissonLock类的tryAcquireAsync方法的实现
            lockInstance.lock(expire, TimeUnit.MILLISECONDS);
            return lockInstance;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 释放锁。注意：必须和获取锁在一个线程中
     *
     * @param lockInstance 锁的实例，acquire返回的
     * @return 释放成功返回true，否则返回false
     */
    public boolean releaseLock(RLock lockInstance) {
        if (lockInstance.isHeldByCurrentThread()) {
            lockInstance.unlock();
            return true;
        }
        return false;
    }
}