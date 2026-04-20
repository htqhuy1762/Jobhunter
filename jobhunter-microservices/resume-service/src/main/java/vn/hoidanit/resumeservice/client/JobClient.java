package vn.hoidanit.resumeservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import vn.hoidanit.resumeservice.domain.response.RestResponse;
import vn.hoidanit.resumeservice.dto.JobDTO;

@FeignClient(name = "job-service")
public interface JobClient {

    @GetMapping("/api/v1/jobs/internal/{id}")
    RestResponse<JobDTO> getJobById(@PathVariable("id") Long id);
}
