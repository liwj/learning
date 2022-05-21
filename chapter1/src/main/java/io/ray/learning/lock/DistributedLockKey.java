package io.ray.learning.lock;

import lombok.Data;

@Data
public abstract class DistributedLockKey<T> {
    protected String appId;
    protected String correlationId;

    public DistributedLockKey(String appId, String correlationId) {
        this.appId = appId;
        this.correlationId = correlationId;
    }

    abstract T serialize();
}
