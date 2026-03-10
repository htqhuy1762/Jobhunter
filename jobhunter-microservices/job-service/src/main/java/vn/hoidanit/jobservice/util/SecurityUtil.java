package vn.hoidanit.jobservice.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

/**
 * Utility class to extract user information from Spring Security Context
 * JWT is verified by each service independently
 */
@Slf4j
public class SecurityUtil {

    public static final String AUTHORITIES_KEY = "permission";
    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS256;

    private SecurityUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Get current logged in user's email from Spring Security Context
     * 
     * @return Optional containing user email
     */
    public static Optional<String> getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractPrincipal(securityContext.getAuthentication()));
    }

    /**
     * Get current user email (for logging and info purposes)
     * 
     * @return User email or "anonymous" if not found
     */
    public static String getCurrentUserEmail() {
        return getCurrentUserLogin().orElse("anonymous");
    }

    /**
     * Get current user info string (email from JWT)
     * 
     * @return User email or "anonymous"
     */
    public static String getCurrentUserInfo() {
        return getCurrentUserEmail();
    }

    /**
     * Get current user ID from JWT claims
     * 
     * @return User ID or null if not found
     */
    public static Long getCurrentUserId() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            Object userId = jwt.getClaim("userId");
            if (userId instanceof Integer) {
                return ((Integer) userId).longValue();
            } else if (userId instanceof Long) {
                return (Long) userId;
            }
        }
        return null;
    }

    /**
     * Check if current user has a specific role/authority
     * 
     * @param role Role name to check
     * @return true if user has the role, false otherwise
     */
    public static boolean hasRole(String role) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals(role));
    }

    /**
     * Check if current user is authenticated
     * 
     * @return true if authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    /**
     * Get JWT from authentication
     * 
     * @return Optional containing JWT token string
     */
    public static Optional<String> getCurrentUserJWT() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional
                .ofNullable(securityContext.getAuthentication())
                .filter(authentication -> authentication.getCredentials() instanceof String)
                .map(authentication -> (String) authentication.getCredentials());
    }

    private static String extractPrincipal(Authentication authentication) {
        if (authentication == null) {
            return null;
        } else if (authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
            return springSecurityUser.getUsername();
        } else if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        } else if (authentication.getPrincipal() instanceof String s) {
            return s;
        }
        return null;
    }
}
