package vn.hoidanit.resumeservice.controller;

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
import vn.hoidanit.resumeservice.annotation.PageableDefault;
import vn.hoidanit.resumeservice.domain.Resume;
import vn.hoidanit.resumeservice.domain.response.RestResponse;
import vn.hoidanit.resumeservice.dto.ReqCreateResumeDTO;
import vn.hoidanit.resumeservice.dto.ReqUpdateResumeDTO;
import vn.hoidanit.resumeservice.dto.ResCreateResumeDTO;
import vn.hoidanit.resumeservice.dto.ResFetchResumeDTO;
import vn.hoidanit.resumeservice.dto.ResUpdateResumeDTO;
import vn.hoidanit.resumeservice.dto.ResultPaginationDTO;
import vn.hoidanit.resumeservice.service.ResumeService;
import vn.hoidanit.resumeservice.util.SecurityUtil;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ResumeController {
    private final ResumeService resumeService;

    @PostMapping("/resumes")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_HR', 'ROLE_ADMIN')")
    public ResponseEntity<RestResponse<ResCreateResumeDTO>> create(@Valid @RequestBody ReqCreateResumeDTO reqDto) {
        boolean isAdmin = SecurityUtil.hasRole("ROLE_ADMIN");
        boolean isHr = SecurityUtil.hasRole("ROLE_HR");
        boolean isUser = SecurityUtil.hasRole("ROLE_USER");

        if (isUser && !isHr && !isAdmin) {
            Long currentUserId = SecurityUtil.getCurrentUserId();
            if (currentUserId == null) {
                return RestResponse.error(HttpStatus.FORBIDDEN,
                        "Unable to resolve current user from authentication context");
            }

            if (reqDto.getUser() == null || reqDto.getUser().getId() == null
                    || !currentUserId.equals(reqDto.getUser().getId())) {
                return RestResponse.error(HttpStatus.FORBIDDEN,
                        "You can only submit resume for your own account");
            }

            String currentUserEmail = SecurityUtil.getCurrentUserEmail();
            if (currentUserEmail != null && !currentUserEmail.isBlank()) {
                reqDto.setEmail(currentUserEmail);
            }
        }

        // Convert DTO to entity
        Resume resume = new Resume();
        resume.setEmail(reqDto.getEmail());
        resume.setUrl(reqDto.getUrl());
        resume.setStatus(reqDto.getStatus());
        resume.setUserId(reqDto.getUser().getId());
        resume.setJobId(reqDto.getJob().getId());

        // Check if user and job exist
        boolean isValid = this.resumeService.checkResumeExistByUserAndJob(resume);
        if (!isValid) {
            return RestResponse.error(HttpStatus.BAD_REQUEST,
                    "Invalid user or job. Please check userId and jobId.");
        }

        // Create resume
        ResCreateResumeDTO createdResume = this.resumeService.create(resume);
        return RestResponse.created(createdResume, "Create resume successfully");
    }

    @PutMapping("/resumes")
    @PreAuthorize("hasAnyAuthority('ROLE_HR', 'ROLE_ADMIN')")
    public ResponseEntity<RestResponse<ResUpdateResumeDTO>> update(@Valid @RequestBody ReqUpdateResumeDTO reqDto) {
        // check id exists
        Optional<Resume> resumeOptional = this.resumeService.fetchById(reqDto.getId());
        if (resumeOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Resume reqResume = resumeOptional.get();

        boolean isAdmin = SecurityUtil.hasRole("ROLE_ADMIN");
        boolean isHr = SecurityUtil.hasRole("ROLE_HR");

        if (isHr && !isAdmin) {
            Long currentCompanyId = this.resumeService.getCurrentUserCompanyId();
            if (!this.resumeService.isResumeInCompany(reqResume, currentCompanyId)) {
                return RestResponse.error(HttpStatus.FORBIDDEN,
                        "You don't have permission to update this resume");
            }
        }

        if (reqDto.getStatus() != null
                && !this.resumeService.canTransition(reqResume.getStatus(), reqDto.getStatus())) {
            return RestResponse.error(HttpStatus.BAD_REQUEST,
                    String.format("Invalid resume status transition: %s -> %s", reqResume.getStatus(),
                            reqDto.getStatus()));
        }

        if (reqDto.getStatus() != null) {
            reqResume.setStatus(reqDto.getStatus());
        }

        reqResume.setRating(reqDto.getRating());
        reqResume.setNotes(reqDto.getNotes());
        reqResume.setInterviewDateTime(reqDto.getInterviewDateTime());
        reqResume.setInterviewer(reqDto.getInterviewer());
        reqResume.setMeetingType(reqDto.getMeetingType());
        reqResume.setMeetingLink(reqDto.getMeetingLink());
        reqResume.setMeetingLocation(reqDto.getMeetingLocation());
        reqResume.setInterviewNote(reqDto.getInterviewNote());
        reqResume.setInterviewResult(reqDto.getInterviewResult());

        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId != null) {
            reqResume.setReviewedBy(currentUserId);
            reqResume.setReviewedAt(java.time.Instant.now());
        }

        ResUpdateResumeDTO updatedResume = this.resumeService.update(reqResume);
        return RestResponse.ok(updatedResume, "Update resume successfully");
    }

    @DeleteMapping("/resumes/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_HR', 'ROLE_ADMIN')")
    public ResponseEntity<RestResponse<Void>> delete(@PathVariable("id") long id) {
        Optional<Resume> resumeOptional = this.resumeService.fetchById(id);
        if (resumeOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Resume resume = resumeOptional.get();

        // Data-level authorization: USER can only delete their own resumes
        Long currentUserId = vn.hoidanit.resumeservice.util.SecurityUtil.getCurrentUserId();
        boolean isHrOrAdmin = vn.hoidanit.resumeservice.util.SecurityUtil.hasRole("ROLE_HR") ||
                vn.hoidanit.resumeservice.util.SecurityUtil.hasRole("ROLE_ADMIN");

        boolean isOwner = currentUserId != null && currentUserId.equals(resume.getUserId());

        if (vn.hoidanit.resumeservice.util.SecurityUtil.hasRole("ROLE_HR")
                && !vn.hoidanit.resumeservice.util.SecurityUtil.hasRole("ROLE_ADMIN")) {
            Long currentCompanyId = this.resumeService.getCurrentUserCompanyId();
            boolean inSameCompany = this.resumeService.isResumeInCompany(resume, currentCompanyId);
            if (!inSameCompany) {
                return RestResponse.error(org.springframework.http.HttpStatus.FORBIDDEN,
                        "You don't have permission to delete this resume");
            }
        }

        if (!isHrOrAdmin && !isOwner) {
            return RestResponse.error(org.springframework.http.HttpStatus.FORBIDDEN,
                    "You don't have permission to delete this resume");
        }

        this.resumeService.delete(id);
        return RestResponse.ok(null, "Delete resume successfully");
    }

    @GetMapping("/resumes/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_HR', 'ROLE_ADMIN')")
    public ResponseEntity<RestResponse<ResFetchResumeDTO>> fetchById(@PathVariable("id") long id) {
        Optional<Resume> resumeOptional = this.resumeService.fetchById(id);
        if (resumeOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Resume resume = resumeOptional.get();

        // Data-level authorization: USER can only view their own resumes
        Long currentUserId = vn.hoidanit.resumeservice.util.SecurityUtil.getCurrentUserId();
        boolean isHrOrAdmin = vn.hoidanit.resumeservice.util.SecurityUtil.hasRole("ROLE_HR") ||
                vn.hoidanit.resumeservice.util.SecurityUtil.hasRole("ROLE_ADMIN");

        boolean isOwner = currentUserId != null && currentUserId.equals(resume.getUserId());

        if (vn.hoidanit.resumeservice.util.SecurityUtil.hasRole("ROLE_HR")
                && !vn.hoidanit.resumeservice.util.SecurityUtil.hasRole("ROLE_ADMIN")) {
            Long currentCompanyId = this.resumeService.getCurrentUserCompanyId();
            boolean inSameCompany = this.resumeService.isResumeInCompany(resume, currentCompanyId);
            if (!inSameCompany) {
                return RestResponse.error(org.springframework.http.HttpStatus.FORBIDDEN,
                        "You don't have permission to view this resume");
            }
        }

        if (!isHrOrAdmin && !isOwner) {
            return RestResponse.error(org.springframework.http.HttpStatus.FORBIDDEN,
                    "You don't have permission to view this resume");
        }

        ResFetchResumeDTO resumeDTO = this.resumeService.getResume(resume);
        return RestResponse.ok(resumeDTO, "Fetch resume by id successfully");
    }

    @GetMapping("/resumes")
    @PreAuthorize("hasAnyAuthority('ROLE_HR', 'ROLE_ADMIN')")
    public ResponseEntity<RestResponse<ResultPaginationDTO>> fetchAll(
            @Filter Specification<Resume> spec,
            @PageableDefault(page = 1, size = 10, sort = "id", direction = "desc") Pageable pageable) {

        ResultPaginationDTO result;
        if (SecurityUtil.hasRole("ROLE_HR") && !SecurityUtil.hasRole("ROLE_ADMIN")) {
            Long currentCompanyId = this.resumeService.getCurrentUserCompanyId();
            if (currentCompanyId == null) {
                return RestResponse.error(HttpStatus.FORBIDDEN,
                        "Current HR account is not associated with any company");
            }
            result = this.resumeService.fetchAllResumeForCompany(spec, pageable, currentCompanyId);
        } else {
            result = this.resumeService.fetchAllResume(spec, pageable);
        }
        return RestResponse.ok(result, "Fetch resumes successfully");
    }

    @GetMapping("/resumes/by-user")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_HR', 'ROLE_ADMIN')")
    public ResponseEntity<RestResponse<ResultPaginationDTO>> fetchByUser(
            @PageableDefault(page = 1, size = 10, sort = "id", direction = "desc") Pageable pageable) {

        ResultPaginationDTO result = this.resumeService.fetchAllResumeByUser(pageable);
        return RestResponse.ok(result, "Fetch resumes by user successfully");
    }
}
