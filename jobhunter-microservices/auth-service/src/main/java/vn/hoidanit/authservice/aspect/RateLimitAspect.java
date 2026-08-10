package vn.hoidanit.authservice.aspect;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import vn.hoidanit.authservice.annotation.RateLimit;
import vn.hoidanit.authservice.config.Bucket4jProperties;
import vn.hoidanit.authservice.util.SecurityUtil;

import java.nio.charset.StandardCharsets;

/**
 * Aspect to handle @RateLimit annotation.
 * Backed by a distributed Bucket4j bucket (Redis), so the limit holds even
 * when auth-service is scaled to multiple instances - unlike an in-memory
 * limiter, where each instance would enforce the configured limit independently.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitAspect {

    private static final String BUCKET_KEY_PREFIX = "rate-limit:";

    private final ProxyManager<byte[]> bucket4jProxyManager;
    private final Bucket4jProperties bucket4jProperties;

    @Around("@annotation(rateLimit)")
    public Object aroundRateLimitedMethod(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String rateLimiterName = rateLimit.name();
        Bucket4jProperties.Instance config = bucket4jProperties.getInstances().get(rateLimiterName);

        if (config == null) {
            log.warn("No bucket4j configuration found for rate limiter '{}'; request allowed without limiting",
                    rateLimiterName);
            return joinPoint.proceed();
        }

        byte[] key = (BUCKET_KEY_PREFIX + rateLimiterName).getBytes(StandardCharsets.UTF_8);
        BucketProxy bucket = bucket4jProxyManager.builder()
                .build(key, () -> BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(config.getCapacity(), config.getRefillPeriod()))
                        .build());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        String userEmail = SecurityUtil.getCurrentUserLogin().orElse("anonymous");

        if (probe.isConsumed()) {
            log.debug("Rate limit check passed for user: {} on {}", userEmail, rateLimiterName);
            return joinPoint.proceed();
        }

        log.warn("Rate limit exceeded for user: {} on {}", userEmail, rateLimiterName);
        throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Rate limit exceeded. Please try again later."
        );
    }
}
