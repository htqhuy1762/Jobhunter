package vn.hoidanit.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import vn.hoidanit.gateway.service.TokenBlacklistService;
import vn.hoidanit.gateway.util.SignatureUtil;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_EMAIL = "X-User-Email";
    private static final String HEADER_USER_ROLES = "X-User-Roles";
    private static final String HEADER_GATEWAY_SIGNATURE = "X-Gateway-Signature";
    private static final String HEADER_GATEWAY_TIMESTAMP = "X-Gateway-Timestamp";

    private static final String CLAIM_USER = "user";
    private static final String CLAIM_PERMISSION = "permission";
    private static final String USER_ID_KEY = "id";

    @Value("${hoidanit.jwt.base64-secret}")
    private String jwtSecret;

    @Value("${gateway.signature.secret}")
    private String gatewaySignatureSecret;

    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationFilter(TokenBlacklistService tokenBlacklistService) {
        super(Config.class);
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Skip JWT validation for OPTIONS requests (CORS preflight)
            if ("OPTIONS".equalsIgnoreCase(request.getMethod().name())) {
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(BEARER_PREFIX_LENGTH);

            try {
                Claims claims = parseToken(token);
                String email = claims.getSubject();
                String userId = extractUserId(claims);
                String roles = extractRoles(claims);

                return tokenBlacklistService.isAccessTokenBlacklisted(token)
                        .flatMap(isBlacklisted -> {
                            if (Boolean.TRUE.equals(isBlacklisted)) {
                                log.warn("Blacklisted token attempt for user: {}", email);
                                return onError(exchange, "Token has been revoked. Please login again.",
                                        HttpStatus.UNAUTHORIZED);
                            }

                            ServerHttpRequest modifiedRequest = buildAuthenticatedRequest(request, userId, email,
                                    roles);
                            log.debug("JWT validated for user: {} (roles: {})", email, roles);

                            return chain.filter(exchange.mutate().request(modifiedRequest).build());
                        });

            } catch (Exception e) {
                log.error("JWT validation failed: {}", e.getMessage());
                return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Claims parseToken(String token) {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String extractUserId(Claims claims) {
        Object userObj = claims.get(CLAIM_USER);
        if (userObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> userMap = (Map<String, Object>) userObj;
            Object idObj = userMap.get(USER_ID_KEY);
            return idObj != null ? String.valueOf(idObj) : null;
        }
        return null;
    }

    private String extractRoles(Claims claims) {
        Object permissionsObj = claims.get(CLAIM_PERMISSION);
        if (permissionsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> permissions = (List<String>) permissionsObj;
            return String.join(",", permissions);
        }
        return "";
    }

    private ServerHttpRequest buildAuthenticatedRequest(ServerHttpRequest request, String userId, String email,
            String roles) {
        long timestamp = System.currentTimeMillis();
        String signatureData = SignatureUtil.createSignatureData(userId, email, timestamp);
        String signature = SignatureUtil.generateSignature(signatureData, gatewaySignatureSecret);

        // Pass through the original Authorization header for downstream JWT validation
        // Also add user headers for backward compatibility and additional context
        return request.mutate()
                .header(HEADER_USER_ID, userId)
                .header(HEADER_USER_EMAIL, email)
                .header(HEADER_USER_ROLES, roles)
                .header(HEADER_GATEWAY_SIGNATURE, signature)
                .header(HEADER_GATEWAY_TIMESTAMP, String.valueOf(timestamp))
                // NOTE: Authorization header is automatically passed through by Spring Cloud
                // Gateway
                .build();
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);

        log.error("Authentication error: {}", err);
        return response.setComplete();
    }

    public static class Config {
    }
}
