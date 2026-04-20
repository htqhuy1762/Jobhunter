package vn.hoidanit.jobservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import vn.hoidanit.jobservice.dto.RestResponseWrapper;
import vn.hoidanit.jobservice.dto.UserDTO;

@FeignClient(name = "auth-service")
public interface UserClient {

    @GetMapping("/api/v1/users/internal/{id}")
    RestResponseWrapper<UserDTO> getUserById(@PathVariable("id") Long id);
}
