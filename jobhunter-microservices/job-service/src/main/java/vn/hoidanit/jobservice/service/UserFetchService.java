package vn.hoidanit.jobservice.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.hoidanit.jobservice.client.UserClient;
import vn.hoidanit.jobservice.dto.RestResponseWrapper;
import vn.hoidanit.jobservice.dto.UserDTO;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserFetchService {

    private final UserClient userClient;

    public UserDTO fetchUser(Long userId) {
        try {
            RestResponseWrapper<UserDTO> response = userClient.getUserById(userId);
            return response != null ? response.getData() : null;
        } catch (Exception ex) {
            log.warn("Failed to fetch user {} from auth-service: {}", userId, ex.getMessage());
            return null;
        }
    }
}
