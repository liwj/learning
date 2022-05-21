package io.ray.learning.lock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPoolConfig;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
@Configuration
public class DistributedLockConfig {

    private static final int REDIS_DATABASE_INDEX = 2;

    @Autowired
    private RedisProperties redisProperties;

    @Value("${lock.expire:30000}")
    private long expireMilliSeconds;

    @Bean
    public RedisLockManager springRedisLockManager() throws UnknownHostException {
        return new RedisLockManager(new SpringRedisLockManager(redisIdempotencyConnectionFactory(), expireMilliSeconds));
    }

    protected JedisConnectionFactory redisIdempotencyConnectionFactory() throws UnknownHostException {

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        JedisConnectionFactory connectionFactory;
        if (getSentinelConfig() != null) {
            connectionFactory = new JedisConnectionFactory(this.getSentinelConfig(), poolConfig);
        } else {
            connectionFactory =  getClusterConfiguration() != null ? new JedisConnectionFactory(getClusterConfiguration(), poolConfig) : new JedisConnectionFactory(poolConfig);
        }

        this.configureConnection(connectionFactory);
        if (redisProperties.isSsl()) {
            connectionFactory.setUseSsl(true);
        }
        connectionFactory.setDatabase(REDIS_DATABASE_INDEX);
        if ( redisProperties.getTimeout() != null && redisProperties.getTimeout().toMillis() > 0) {
            connectionFactory.setTimeout((int) redisProperties.getTimeout().toMillis());
        }

        connectionFactory.afterPropertiesSet();
        return connectionFactory;
    }

    protected final RedisSentinelConfiguration getSentinelConfig() {
        RedisProperties.Sentinel sentinelProperties = redisProperties.getSentinel();
        if (sentinelProperties != null) {
            RedisSentinelConfiguration config = new RedisSentinelConfiguration();
            config.master(sentinelProperties.getMaster());
            config.setSentinels(this.createSentinels(sentinelProperties));
            return config;
        } else {
            return null;
        }
    }

    protected final RedisClusterConfiguration getClusterConfiguration() {
        if (redisProperties.getCluster() == null) {
            return null;
        }
        RedisProperties.Cluster clusterProperties = redisProperties.getCluster();
        RedisClusterConfiguration config = new RedisClusterConfiguration(clusterProperties.getNodes());
        if (clusterProperties.getMaxRedirects() != null) {
            config.setMaxRedirects(clusterProperties.getMaxRedirects().intValue());
        }

        return config;
    }

    private List<RedisNode> createSentinels(RedisProperties.Sentinel sentinel) {
        List<RedisNode> nodes = new ArrayList();
        List stringArray = sentinel.getNodes();
        int length = stringArray.size();

        for(int i = 0; i < length; ++i) {
            String node = (String) stringArray.get(i);

            try {
                String[] parts = StringUtils.split(node, ":");
                Assert.state(parts.length == 2, "Must be defined as 'host:port'");
                nodes.add(new RedisNode(parts[0], Integer.valueOf(parts[1]).intValue()));
            } catch (RuntimeException e) {
                throw new IllegalStateException("Invalid redis sentinel property '" + node + "'", e);
            }
        }

        return nodes;
    }

    private void configureConnection(JedisConnectionFactory factory) {
        if (StringUtils.hasText(redisProperties.getUrl())) {
            this.configureConnectionFromUrl(factory);
        } else {
            factory.setHostName(redisProperties.getHost());
            factory.setPort(redisProperties.getPort());
            if (redisProperties.getPassword() != null) {
                factory.setPassword(redisProperties.getPassword());
            }
        }
    }

    private void configureConnectionFromUrl(JedisConnectionFactory factory) {
        String url = redisProperties.getUrl();
        if (url.startsWith("rediss://")) {
            factory.setUseSsl(true);
        }

        try {
            URI uri = new URI(url);
            factory.setHostName(uri.getHost());
            factory.setPort(uri.getPort());
            if (uri.getUserInfo() != null) {
                String password = uri.getUserInfo();
                int index = password.lastIndexOf(":");
                if (index >= 0) {
                    password = password.substring(index + 1);
                }

                factory.setPassword(password);
            }

        } catch (URISyntaxException var6) {
            throw new IllegalArgumentException("Malformed 'spring.redis.url' " + url, var6);
        }
    }
}
