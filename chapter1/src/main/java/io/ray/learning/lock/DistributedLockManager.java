package io.ray.learning.lock;

import java.util.concurrent.locks.Lock;

/**
 * 分布式锁管理器
 * @param <K>
 */
public interface DistributedLockManager<K extends DistributedLockKey> {

    Lock getDistributedLock(K distributedLockKey);
}
