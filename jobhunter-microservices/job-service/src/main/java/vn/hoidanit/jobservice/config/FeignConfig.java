package vn.hoidanit.jobservice.config;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import vn.hoidanit.jobservice.util.SecurityUtil;
import vn.hoidanit.jobservice.util.SignatureUtil;

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
                Long userId = SecurityUtil.getCurrentUserId();
                String userEmail = SecurityUtil.getCurrentUserEmail();

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String roles = null;
                if (authentication != null && authentication.getAuthorities() != null) {
                    roles = authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.joining(","));
                }

                if (userId != null && userEmail != null) {
                    long timestamp = System.currentTimeMillis();
                    String signatureData = SignatureUtil.createSignatureData(String.valueOf(userId), userEmail,
                            timestamp);
                    String signature = SignatureUtil.generateSignature(signatureData, gatewaySignatureSecret);

                    template.header("X-Gateway-Signature", signature);
                    template.header("X-Gateway-Timestamp", String.valueOf(timestamp));
                    template.header("X-User-Id", String.valueOf(userId));
                    template.header("X-User-Email", userEmail);
                    if (roles != null && !roles.isEmpty()) {
                        template.header("X-User-Roles", roles);
                    }
                    log.debug("Added signed gateway headers to Feign call: {} {}", template.method(), template.url());
                } else {
                    long timestamp = System.currentTimeMillis();
                    String signatureData = SignatureUtil.createSignatureData("0", "system", timestamp);
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
