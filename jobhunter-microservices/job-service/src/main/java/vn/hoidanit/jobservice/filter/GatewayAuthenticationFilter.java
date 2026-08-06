package vn.hoidanit.jobservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.hoidanit.jobservice.util.SignatureUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Filter to authenticate requests from API Gateway
 * Validates Gateway signature instead of JWT tokens
 * This is part of Gateway-Only authentication pattern
 */
@Component
@Slf4j
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_GATEWAY_SIGNATURE = "X-Gateway-Signature";
    private static final String HEADER_GATEWAY_TIMESTAMP = "X-Gateway-Timestamp";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_EMAIL = "X-User-Email";
    private static final String HEADER_USER_ROLES = "X-User-Roles";
    @Value("${gateway.signature.secret}")
    private String gatewaySignatureSecret;

    @Value("${gateway.signature.enabled:true}")
    private boolean gatewaySignatureEnabled;

    @Value("${gateway.signature.timestamp-tolerance-seconds:60}")
    private long timestampToleranceSeconds;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String gatewaySignature = request.getHeader(HEADER_GATEWAY_SIGNATURE);
        String gatewayTimestamp = request.getHeader(HEADER_GATEWAY_TIMESTAMP);
        String userId = request.getHeader(HEADER_USER_ID);
        String userEmail = request.getHeader(HEADER_USER_EMAIL);
        String userRoles = request.getHeader(HEADER_USER_ROLES);

        if (gatewaySignature != null && gatewayTimestamp != null && gatewaySignatureEnabled) {
            log.debug("Validating Gateway signature for user: {}", userEmail);

            try {
                long timestamp = Long.parseLong(gatewayTimestamp);

                // Check timestamp to prevent replay attacks
                long currentTime = System.currentTimeMillis();
                if (Math.abs(currentTime - timestamp) > timestampToleranceSeconds * 1000L) {
                    log.warn("Request timestamp too old or too far in future: {} (current: {})", timestamp,
                            currentTime);
                    sendUnauthorizedResponse(response, "Request timestamp invalid");
                    return;
                }

                String signatureData = SignatureUtil.createSignatureData(userId, userEmail, userRoles, timestamp);

                if (SignatureUtil.verifySignature(signatureData, gatewaySignature, gatewaySignatureSecret)) {
                    log.debug("Gateway signature validated for user: {}", userEmail);

                    List<SimpleGrantedAuthority> authorities = parseAuthorities(userRoles);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userEmail, null, authorities);
                    Map<String, Object> details = new HashMap<>();
                    details.put("userId", userId);
                    details.put("userEmail", userEmail);
                    authentication.setDetails(details);

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    filterChain.doFilter(request, response);
                    return;
                } else {
                    log.error("Invalid Gateway signature - request rejected for user: {}", userEmail);
                    sendUnauthorizedResponse(response, "Invalid Gateway signature");
                    return;
                }
            } catch (NumberFormatException e) {
                log.error("Invalid timestamp format: {}", gatewayTimestamp);
                sendBadRequestResponse(response, "Invalid timestamp format");
                return;
            }
        }

        // No Gateway signature - continue to allow public endpoints
        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> parseAuthorities(String rolesString) {
        if (rolesString == null || rolesString.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(rolesString.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(
                "{\"statusCode\":401,\"error\":\"Unauthorized\",\"message\":\"%s\"}", message));
    }

    private void sendBadRequestResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(
                "{\"statusCode\":400,\"error\":\"Bad Request\",\"message\":\"%s\"}", message));
    }
}
