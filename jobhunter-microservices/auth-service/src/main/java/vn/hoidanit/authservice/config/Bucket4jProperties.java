package vn.hoidanit.authservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-name distributed rate limit configuration, e.g.:
 * bucket4j.instances.login.capacity=5
 * bucket4j.instances.login.refill-period=1m
 * Referenced by name from @RateLimit(name = "...").
 */
@Component
@ConfigurationProperties(prefix = "bucket4j")
@Data
public class Bucket4jProperties {

    private Map<String, Instance> instances = new HashMap<>();

    @Data
    public static class Instance {
        private int capacity;
        private Duration refillPeriod;
    }
}
