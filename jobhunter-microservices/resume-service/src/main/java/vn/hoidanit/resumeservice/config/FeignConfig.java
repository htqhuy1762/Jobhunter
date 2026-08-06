package vn.hoidanit.resumeservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.hoidanit.resumeservice.util.SecurityUtil;
import vn.hoidanit.resumeservice.util.SignatureUtil;

import java.util.stream.Collectors;

@Configuration
@Slf4j
public class FeignConfig {

    @Value("${gateway.signature.secret}")
    private String gatewaySignatureSecret;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // Get current user information from Spring Security Context
                Long userId = SecurityUtil.getCurrentUserId();
                String userEmail = SecurityUtil.getCurrentUserEmail();

                // Extract roles from Spring Security Authentication
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String roles = null;
                if (authentication != null && authentication.getAuthorities() != null) {
                    roles = authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.joining(","));
                }

                if (userId != null && userEmail != null) {
                    // Generate Gateway signature for inter-service communication
                    long timestamp = System.currentTimeMillis();
                    String signatureData = SignatureUtil.createSignatureData(
                            String.valueOf(userId),
                            userEmail,
                            roles,
                            timestamp);
                    String signature = SignatureUtil.generateSignature(signatureData, gatewaySignatureSecret);

                    // Add headers
                    template.header("X-Gateway-Signature", signature);
                    template.header("X-Gateway-Timestamp", String.valueOf(timestamp));
                    template.header("X-User-Id", String.valueOf(userId));
                    template.header("X-User-Email", userEmail);

                    if (roles != null && !roles.isEmpty()) {
                        template.header("X-User-Roles", roles);
                    }

                    log.debug("Added Gateway signature headers to Feign request: {} {} for user: {}",
                            template.method(), template.url(), userEmail);
                } else {
                    log.warn(
                            "No user information found in request headers for Feign request. This may be a system call.");

                    // For system calls without user context, use system credentials
                    long timestamp = System.currentTimeMillis();
                    String signatureData = SignatureUtil.createSignatureData("0", "system", null, timestamp);
                    String signature = SignatureUtil.generateSignature(signatureData, gatewaySignatureSecret);

                    template.header("X-Gateway-Signature", signature);
                    template.header("X-Gateway-Timestamp", String.valueOf(timestamp));
                    template.header("X-User-Id", "0");
                    template.header("X-User-Email", "system");
                }
            }
        };
    }
}
