package io.ray.learning.lock;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;

import java.util.concurrent.locks.Lock;

public class SpringRedisLockManager implements DistributedLockManager<RedisLockKey> {
    private volatile RedisLockRegistry redisLockRegistry;
    private RedisConnectionFactory redisConnectionFactory;
    //锁的过期时间
    private Long expireMs;
    public SpringRedisLockManager(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }
    public SpringRedisLockManager(RedisConnectionFactory redisConnectionFactory, long expireMs) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.expireMs = expireMs;
    }
    @Override
    public Lock getDistributedLock(RedisLockKey distributedLockKey) {
        if (redisLockRegistry == null) {
            synchronized (this) {
                if (redisLockRegistry == null) {
                    if (expireMs != null)
                        redisLockRegistry = new RedisLockRegistry(redisConnectionFactory, distributedLockKey.getAppId(), expireMs);
                    else
                        redisLockRegistry = new RedisLockRegistry(redisConnectionFactory, distributedLockKey.getAppId());
                }
            }
        }
        return redisLockRegistry.obtain(distributedLockKey.serialize());
    }
}
