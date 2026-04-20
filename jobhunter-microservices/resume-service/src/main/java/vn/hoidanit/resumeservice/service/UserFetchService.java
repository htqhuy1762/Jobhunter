package vn.hoidanit.resumeservice.service;

import org.springframework.stereotype.Service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.hoidanit.resumeservice.client.UserClient;
import vn.hoidanit.resumeservice.domain.response.RestResponse;
import vn.hoidanit.resumeservice.dto.UserDTO;

/**
 * Service for fetching user information with Circuit Breaker protection
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserFetchService {

    private final UserClient userClient;

    @CircuitBreaker(name = "userService", fallbackMethod = "fetchUserFallback")
    @Retry(name = "userService")
    public UserDTO fetchUser(Long userId) {
        log.debug("Fetching user with id: {}", userId);
        RestResponse<UserDTO> response = userClient.getUserById(userId);
        return response != null ? response.getData() : null;
    }

    public UserDTO fetchUserFallback(Long userId, Throwable ex) {
        log.warn("Circuit breaker fallback triggered for user {}: {}", userId, ex.getMessage());
        log.debug("Exception type: {}", ex.getClass().getName());
        return null;
    }
}
