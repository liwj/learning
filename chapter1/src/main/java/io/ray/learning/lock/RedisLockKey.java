package io.ray.learning.lock;


public class RedisLockKey extends DistributedLockKey<String> {

    public RedisLockKey(String appId, String correlationId) {
        super(appId, correlationId);
    }

    @Override
    public String serialize() {
        return correlationId;
    }
}
