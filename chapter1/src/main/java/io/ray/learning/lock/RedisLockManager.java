package io.ray.learning.lock;

import java.util.concurrent.locks.Lock;

public class RedisLockManager implements DistributedLockManager<RedisLockKey> {

    private DistributedLockManager<RedisLockKey> delegateImplementation;

    public RedisLockManager(DistributedLockManager<RedisLockKey> delegateImplementation) {
        this.delegateImplementation = delegateImplementation;
    }

    @Override
    public Lock getDistributedLock(RedisLockKey distributedLockKey) {
        return delegateImplementation.getDistributedLock(distributedLockKey);
    }
}
