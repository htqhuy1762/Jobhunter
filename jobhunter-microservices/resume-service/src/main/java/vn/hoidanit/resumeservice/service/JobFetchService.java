package vn.hoidanit.resumeservice.service;

import org.springframework.stereotype.Service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.hoidanit.resumeservice.client.JobClient;
import vn.hoidanit.resumeservice.domain.response.RestResponse;
import vn.hoidanit.resumeservice.dto.JobDTO;

/**
 * Service for fetching job information with Circuit Breaker protection
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobFetchService {

    private final JobClient jobClient;

    @CircuitBreaker(name = "jobService", fallbackMethod = "fetchJobFallback")
    @Retry(name = "jobService")
    public JobDTO fetchJob(Long jobId) {
        log.debug("Fetching job with id: {}", jobId);
        RestResponse<JobDTO> response = jobClient.getJobById(jobId);
        return response != null ? response.getData() : null;
    }

    public JobDTO fetchJobFallback(Long jobId, Throwable ex) {
        log.warn("Circuit breaker fallback triggered for job {}: {}", jobId, ex.getMessage());
        log.debug("Exception type: {}", ex.getClass().getName());
        return null;
    }
}
