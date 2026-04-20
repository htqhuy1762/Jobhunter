package vn.hoidanit.jobservice.controller;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import vn.hoidanit.jobservice.annotation.PageableDefault;
import vn.hoidanit.jobservice.domain.Job;
import vn.hoidanit.jobservice.domain.response.RestResponse;
import vn.hoidanit.jobservice.dto.ResCreateJobDTO;
import vn.hoidanit.jobservice.dto.ResJobDTO;
import vn.hoidanit.jobservice.dto.ResUpdateJobDTO;
import vn.hoidanit.jobservice.dto.ResultPaginationDTO;
import vn.hoidanit.jobservice.service.JobService;
import vn.hoidanit.jobservice.util.SecurityUtil;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class JobController {
    private final JobService jobService;

    @PostMapping("/jobs")
    @PreAuthorize("hasAnyAuthority('ROLE_HR', 'ROLE_ADMIN')")
    public ResponseEntity<RestResponse<ResCreateJobDTO>> create(@Valid @RequestBody Job job) {
        if (SecurityUtil.hasRole("ROLE_HR") && !SecurityUtil.hasRole("ROLE_ADMIN")) {
            Long currentCompanyId = this.jobService.getCurrentUserCompanyId();
            if (currentCompanyId == null) {
                return RestResponse.error(HttpStatus.FORBIDDEN,
                        "Current HR account is not associated with any company");
            }

            if (job.getCompanyId() == null) {
                job.setCompanyId(currentCompanyId);
            } else if (!currentCompanyId.equals(job.getCompanyId())) {
                return RestResponse.error(HttpStatus.FORBIDDEN,
                        "You don't have permission to create jobs for another company");
            }
        }

        ResCreateJobDTO createdJob = this.jobService.create(job);
        return RestResponse.created(createdJob, "Create job successfully");
    }

    @PutMapping("/jobs")
    @PreAuthorize("hasAnyAuthority('ROLE_HR', 'ROLE_ADMIN')")
    public ResponseEntity<RestResponse<ResUpdateJobDTO>> update(@Valid @RequestBody Job job) {
        Optional<Job> currentJob = this.jobService.fetchJobById(job.getId());
        if (!currentJob.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        if (SecurityUtil.hasRole("ROLE_HR") && !SecurityUtil.hasRole("ROLE_ADMIN")) {
            Long currentCompanyId = this.jobService.getCurrentUserCompanyId();
            if (currentCompanyId == null || !this.jobService.isJobInCompany(currentJob.get(), currentCompanyId)) {
                return RestResponse.error(HttpStatus.FORBIDDEN,
                        "You don't have permission to update this job");
            }

            if (job.getCompanyId() != null && !currentCompanyId.equals(job.getCompanyId())) {
                return RestResponse.error(HttpStatus.FORBIDDEN,
                        "You can't move a job to another company");
            }
            job.setCompanyId(currentCompanyId);
        }

        ResUpdateJobDTO updatedJob = this.jobService.update(job, currentJob.get());
        return RestResponse.ok(updatedJob, "Update job successfully");
    }

    @DeleteMapping("/jobs/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_HR', 'ROLE_ADMIN')")
    public ResponseEntity<RestResponse<Void>> delete(@PathVariable("id") long id) {
        Optional<Job> currentJob = this.jobService.fetchJobById(id);
        if (!currentJob.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        if (SecurityUtil.hasRole("ROLE_HR") && !SecurityUtil.hasRole("ROLE_ADMIN")) {
            Long currentCompanyId = this.jobService.getCurrentUserCompanyId();
            if (currentCompanyId == null || !this.jobService.isJobInCompany(currentJob.get(), currentCompanyId)) {
                return RestResponse.error(HttpStatus.FORBIDDEN,
                        "You don't have permission to delete this job");
            }
        }

        this.jobService.delete(id);
        return RestResponse.ok(null, "Delete job successfully");
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<RestResponse<ResJobDTO>> getJobById(@PathVariable("id") long id) {
        Optional<Job> currentJob = this.jobService.fetchJobById(id);
        if (currentJob.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (SecurityUtil.hasRole("ROLE_HR") && !SecurityUtil.hasRole("ROLE_ADMIN")) {
            Long currentCompanyId = this.jobService.getCurrentUserCompanyId();
            if (currentCompanyId == null || !this.jobService.isJobInCompany(currentJob.get(), currentCompanyId)) {
                return RestResponse.error(HttpStatus.FORBIDDEN,
                        "You don't have permission to view this job");
            }
        }

        ResJobDTO jobDTO = this.jobService.fetchJobByIdWithCompany(id);
        if (jobDTO == null) {
            return ResponseEntity.notFound().build();
        }

        return RestResponse.ok(jobDTO, "Fetch job by id successfully");
    }

    @GetMapping("/jobs/internal/{id}")
    public ResponseEntity<RestResponse<ResJobDTO>> getJobByIdInternal(@PathVariable("id") long id) {
        ResJobDTO jobDTO = this.jobService.fetchJobByIdWithCompany(id);
        if (jobDTO == null) {
            return ResponseEntity.notFound().build();
        }

        return RestResponse.ok(jobDTO, "Fetch job by id successfully (internal)");
    }

    @GetMapping("/jobs")
    public ResponseEntity<RestResponse<ResultPaginationDTO>> getAllJob(
            @Filter Specification<Job> spec,
            @PageableDefault(page = 1, size = 10, sort = "id", direction = "desc") Pageable pageable) {

        ResultPaginationDTO result;
        if (SecurityUtil.hasRole("ROLE_HR") && !SecurityUtil.hasRole("ROLE_ADMIN")) {
            Long currentCompanyId = this.jobService.getCurrentUserCompanyId();
            if (currentCompanyId == null) {
                return RestResponse.error(HttpStatus.FORBIDDEN,
                        "Current HR account is not associated with any company");
            }
            result = this.jobService.fetchAllForCompany(spec, pageable, currentCompanyId);
        } else {
            result = this.jobService.fetchAll(spec, pageable);
        }
        return RestResponse.ok(result, "Fetch jobs successfully");
    }
}