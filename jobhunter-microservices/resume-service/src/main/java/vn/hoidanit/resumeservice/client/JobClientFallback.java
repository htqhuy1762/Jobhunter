package vn.hoidanit.resumeservice.client;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import vn.hoidanit.resumeservice.domain.response.RestResponse;
import vn.hoidanit.resumeservice.dto.JobDTO;

@Component
@Slf4j
public class JobClientFallback implements JobClient {

    @Override
    public RestResponse<JobDTO> getJobById(Long id) {
        log.error("Fallback triggered for getJobById with id: {}", id);

        JobDTO fallbackJob = new JobDTO();
        fallbackJob.setId(id);
        fallbackJob.setName("Job information unavailable");

        RestResponse<JobDTO> response = new RestResponse<>();
        response.setStatusCode(200);
        response.setData(fallbackJob);
        response.setMessage("Fallback job response");
        return response;
    }
}