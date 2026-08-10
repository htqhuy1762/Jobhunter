package vn.hoidanit.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class RateLimitConfig {

    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String UNKNOWN_CLIENT_KEY = "unknown";

    /**
     * IP-based rate limiting key resolver.
     * Prefers X-Forwarded-For (set by an upstream reverse proxy/load balancer)
     * so clients aren't all bucketed under the proxy's IP; falls back to the
     * socket's remote address, and finally a shared bucket if neither is available
     * instead of throwing.
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(resolveIp(exchange));
    }

    /**
     * User-based rate limiting key resolver, for routes placed after
     * JwtAuthenticationFilter in the filter chain (which sets X-User-Id on the
     * request before this resolver runs). Limits abuse per account regardless of
     * which IP it comes from, instead of only per IP. Falls back to IP-based
     * keying for requests without a resolved user (e.g. anonymous access).
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst(HEADER_USER_ID);
            if (userId != null && !userId.isBlank()) {
                return Mono.just(userId);
            }
            return Mono.just(resolveIp(exchange));
        };
    }

    private static String resolveIp(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest().getHeaders().getFirst(HEADER_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return UNKNOWN_CLIENT_KEY;
        }

        return remoteAddress.getAddress().getHostAddress();
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }
}

