package vn.hoidanit.resumeservice.client;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import vn.hoidanit.resumeservice.domain.response.RestResponse;
import vn.hoidanit.resumeservice.dto.UserDTO;

@Component
@Slf4j
public class UserClientFallback implements UserClient {

    @Override
    public RestResponse<UserDTO> getUserById(Long id) {
        log.error("Fallback triggered for getUserById with id: {}", id);

        UserDTO fallbackUser = new UserDTO();
        fallbackUser.setId(id);
        fallbackUser.setName("User information unavailable");

        RestResponse<UserDTO> response = new RestResponse<>();
        response.setStatusCode(200);
        response.setData(fallbackUser);
        response.setMessage("Fallback user response");
        return response;
    }
}
