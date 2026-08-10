package vn.hoidanit.authservice.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Wires a distributed Bucket4j ProxyManager backed by Redis, using a dedicated
 * Lettuce RedisClient (separate from Spring Data Redis's own connection factory,
 * since Bucket4j's Lettuce integration needs direct access to a RedisClient).
 * Buckets are stored in the same Redis instance already used elsewhere in the
 * stack, so limits stay consistent across all auth-service replicas.
 */
@Configuration
public class Bucket4jRedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean(destroyMethod = "shutdown")
    public RedisClient bucket4jRedisClient() {
        RedisURI.Builder uriBuilder = RedisURI.Builder.redis(redisHost, redisPort);
        if (redisPassword != null && !redisPassword.isBlank()) {
            uriBuilder.withPassword(redisPassword.toCharArray());
        }
        return RedisClient.create(uriBuilder.build());
    }

    @Bean
    public ProxyManager<byte[]> bucket4jProxyManager(RedisClient redisClient) {
        return LettuceBasedProxyManager.builderFor(redisClient)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10)))
                .build();
    }
}
