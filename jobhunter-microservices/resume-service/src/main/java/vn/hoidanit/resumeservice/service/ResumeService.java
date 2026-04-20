package vn.hoidanit.resumeservice.service;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.hoidanit.resumeservice.domain.Resume;
import vn.hoidanit.resumeservice.dto.JobDTO;
import vn.hoidanit.resumeservice.dto.ResCreateResumeDTO;
import vn.hoidanit.resumeservice.dto.ResFetchResumeDTO;
import vn.hoidanit.resumeservice.dto.ResUpdateResumeDTO;
import vn.hoidanit.resumeservice.dto.ResumeApplicationEvent;
import vn.hoidanit.resumeservice.dto.ResultPaginationDTO;
import vn.hoidanit.resumeservice.dto.UserDTO;
import vn.hoidanit.resumeservice.kafka.producer.ResumeEventProducer;
import vn.hoidanit.resumeservice.repository.ResumeRepository;
import vn.hoidanit.resumeservice.util.SecurityUtil;
import vn.hoidanit.resumeservice.util.constant.ResumeStateEnum;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {
    private final ResumeRepository resumeRepository;
    private final UserFetchService userFetchService;
    private final JobFetchService jobFetchService;
    private final ResumeEventProducer resumeEventProducer;

    private static final Map<ResumeStateEnum, Set<ResumeStateEnum>> VALID_TRANSITIONS = Map.of(
            ResumeStateEnum.PENDING,
            Set.of(ResumeStateEnum.REVIEWING, ResumeStateEnum.REJECTED, ResumeStateEnum.WITHDRAWN),
            ResumeStateEnum.REVIEWING,
            Set.of(ResumeStateEnum.SHORTLISTED, ResumeStateEnum.APPROVED, ResumeStateEnum.REJECTED,
                    ResumeStateEnum.WITHDRAWN),
            ResumeStateEnum.SHORTLISTED,
            Set.of(ResumeStateEnum.INTERVIEW_SCHEDULED, ResumeStateEnum.REJECTED, ResumeStateEnum.WITHDRAWN),
            ResumeStateEnum.INTERVIEW_SCHEDULED,
            Set.of(ResumeStateEnum.INTERVIEWED, ResumeStateEnum.REJECTED, ResumeStateEnum.WITHDRAWN),
            ResumeStateEnum.INTERVIEWED,
            Set.of(ResumeStateEnum.OFFERED, ResumeStateEnum.REJECTED, ResumeStateEnum.WITHDRAWN),
            ResumeStateEnum.OFFERED, Set.of(ResumeStateEnum.HIRED, ResumeStateEnum.REJECTED, ResumeStateEnum.WITHDRAWN),
            ResumeStateEnum.APPROVED, Set.of(ResumeStateEnum.HIRED, ResumeStateEnum.REJECTED),
            ResumeStateEnum.HIRED, Set.of(),
            ResumeStateEnum.REJECTED, Set.of(),
            ResumeStateEnum.WITHDRAWN, Set.of());

    public boolean canTransition(ResumeStateEnum current, ResumeStateEnum target) {
        if (target == null || current == target) {
            return true;
        }
        if (current == null) {
            return target == ResumeStateEnum.PENDING;
        }

        return VALID_TRANSITIONS.getOrDefault(current, Set.of()).contains(target);
    }

    public Optional<Resume> fetchById(long id) {
        return this.resumeRepository.findById(id);
    }

    public boolean checkResumeExistByUserAndJob(Resume resume) {
        // Validate userId and jobId are not null
        if (resume.getUserId() == null || resume.getJobId() == null) {
            return false;
        }

        try {
            UserDTO user = userFetchService.fetchUser(resume.getUserId());
            if (user == null || user.getId() == null) {
                log.error("User with id {} not found", resume.getUserId());
                return false;
            }

            JobDTO job = jobFetchService.fetchJob(resume.getJobId());
            if (job == null || job.getId() == null) {
                log.error("Job with id {} not found", resume.getJobId());
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Error calling other services: {}", e.getMessage());
            return false;
        }
    }

    public ResCreateResumeDTO create(Resume resume) {
        resume = this.resumeRepository.save(resume);

        // Publish Kafka event for async processing
        publishResumeSubmittedEvent(resume);

        ResCreateResumeDTO res = new ResCreateResumeDTO();
        res.setId(resume.getId());
        res.setCreatedAt(resume.getCreatedAt());
        res.setCreatedBy(resume.getCreatedBy());

        return res;
    }

    private void publishResumeSubmittedEvent(Resume resume) {
        try {
            // Fetch additional info for the event
            JobDTO job = resume.getJobId() != null ? jobFetchService.fetchJob(resume.getJobId()) : null;

            ResumeApplicationEvent event = ResumeApplicationEvent.builder()
                    .resumeId(resume.getId())
                    .jobId(resume.getJobId())
                    .userId(resume.getUserId())
                    .companyId(job != null && job.getCompany() != null ? job.getCompany().getId() : null)
                    .userEmail(resume.getEmail())
                    .jobName(job != null ? job.getName() : "Unknown Job")
                    .companyName(
                            job != null && job.getCompany() != null ? job.getCompany().getName() : "Unknown Company")
                    .resumeUrl(resume.getUrl())
                    .build();

            resumeEventProducer.publishResumeSubmittedEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish resume submitted event, but resume was saved successfully", e);
            // Don't fail the request if event publishing fails
        }
    }

    public ResUpdateResumeDTO update(Resume resume) {
        resume = this.resumeRepository.save(resume);

        // Publish event if status changed (APPROVED/REJECTED)
        if (resume.getStatus() != null) {
            publishResumeStatusChangeEvent(resume);
        }

        ResUpdateResumeDTO res = new ResUpdateResumeDTO();
        res.setId(resume.getId());
        res.setStatus(resume.getStatus());
        res.setUpdatedAt(resume.getUpdatedAt());
        res.setUpdatedBy(resume.getUpdatedBy());
        res.setRating(resume.getRating());
        res.setNotes(resume.getNotes());
        res.setReviewedBy(resume.getReviewedBy());
        res.setReviewedAt(resume.getReviewedAt());
        res.setInterviewDateTime(resume.getInterviewDateTime());
        res.setInterviewer(resume.getInterviewer());
        res.setMeetingType(resume.getMeetingType());
        res.setMeetingLink(resume.getMeetingLink());
        res.setMeetingLocation(resume.getMeetingLocation());
        res.setInterviewNote(resume.getInterviewNote());
        res.setInterviewResult(resume.getInterviewResult());

        return res;
    }

    private void publishResumeStatusChangeEvent(Resume resume) {
        try {
            JobDTO job = resume.getJobId() != null ? jobFetchService.fetchJob(resume.getJobId()) : null;

            ResumeApplicationEvent.EventType eventType = switch (resume.getStatus()) {
                case APPROVED -> ResumeApplicationEvent.EventType.RESUME_APPROVED;
                case REJECTED -> ResumeApplicationEvent.EventType.RESUME_REJECTED;
                default -> null;
            };

            if (eventType != null) {
                ResumeApplicationEvent event = ResumeApplicationEvent.builder()
                        .eventType(eventType)
                        .resumeId(resume.getId())
                        .jobId(resume.getJobId())
                        .userId(resume.getUserId())
                        .companyId(job != null && job.getCompany() != null ? job.getCompany().getId() : null)
                        .userEmail(resume.getEmail())
                        .jobName(job != null ? job.getName() : "Unknown Job")
                        .companyName(job != null && job.getCompany() != null ? job.getCompany().getName()
                                : "Unknown Company")
                        .resumeUrl(resume.getUrl())
                        .build();

                resumeEventProducer.publishResumeStatusChangeEvent(event);
            }
        } catch (Exception e) {
            log.error("Failed to publish resume status change event", e);
        }
    }

    public void delete(long id) {
        this.resumeRepository.deleteById(id);
    }

    public ResFetchResumeDTO getResume(Resume resume) {
        ResFetchResumeDTO res = new ResFetchResumeDTO();
        res.setId(resume.getId());
        res.setEmail(resume.getEmail());
        res.setUrl(resume.getUrl());
        res.setStatus(resume.getStatus());
        res.setCreatedAt(resume.getCreatedAt());
        res.setUpdatedAt(resume.getUpdatedAt());
        res.setCreatedBy(resume.getCreatedBy());
        res.setUpdatedBy(resume.getUpdatedBy());
        res.setRating(resume.getRating());
        res.setNotes(resume.getNotes());
        res.setReviewedBy(resume.getReviewedBy());
        res.setReviewedAt(resume.getReviewedAt());
        res.setInterviewDateTime(resume.getInterviewDateTime());
        res.setInterviewer(resume.getInterviewer());
        res.setMeetingType(resume.getMeetingType());
        res.setMeetingLink(resume.getMeetingLink());
        res.setMeetingLocation(resume.getMeetingLocation());
        res.setInterviewNote(resume.getInterviewNote());
        res.setInterviewResult(resume.getInterviewResult());

        // In microservices, we need to fetch user and job info via inter-service
        // communication
        // For now, just set basic info with IDs
        if (resume.getUserId() != null) {
            res.setUser(new ResFetchResumeDTO.UserResume(resume.getUserId(), "User #" + resume.getUserId()));
        }
        if (resume.getJobId() != null) {
            res.setJob(new ResFetchResumeDTO.JobResume(resume.getJobId(), "Job #" + resume.getJobId()));
        }

        return res;
    }

    public ResultPaginationDTO fetchAllResume(Specification<Resume> spec, Pageable pageable) {
        Page<Resume> pageUser = this.resumeRepository.findAll(spec, pageable);
        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();

        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());

        meta.setPages(pageUser.getTotalPages());
        meta.setTotal(pageUser.getTotalElements());

        result.setMeta(meta);

        // remove sensitive information
        List<ResFetchResumeDTO> listResume = pageUser.getContent().stream().map(item -> this.getResume(item))
                .collect(Collectors.toList());
        result.setResult(listResume);

        return result;
    }

    public Long getCurrentUserCompanyId() {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            return null;
        }

        UserDTO currentUser = userFetchService.fetchUser(currentUserId);
        if (currentUser == null || currentUser.getCompany() == null) {
            return null;
        }

        return currentUser.getCompany().getId();
    }

    public boolean isResumeInCompany(Resume resume, Long companyId) {
        if (resume == null || companyId == null || resume.getJobId() == null) {
            return false;
        }

        JobDTO job = jobFetchService.fetchJob(resume.getJobId());
        return job != null && job.getCompany() != null && companyId.equals(job.getCompany().getId());
    }

    public ResultPaginationDTO fetchAllResumeForCompany(Specification<Resume> spec, Pageable pageable, Long companyId) {
        List<Resume> allResumes = this.resumeRepository.findAll(spec);
        Map<Long, Long> jobCompanyCache = new HashMap<>();

        List<Resume> scopedResumes = allResumes.stream().filter(resume -> {
            if (resume.getJobId() == null) {
                return false;
            }

            Long scopedCompanyId = jobCompanyCache.get(resume.getJobId());
            if (scopedCompanyId == null) {
                JobDTO job = jobFetchService.fetchJob(resume.getJobId());
                if (job == null || job.getCompany() == null) {
                    jobCompanyCache.put(resume.getJobId(), -1L);
                    return false;
                }
                scopedCompanyId = job.getCompany().getId();
                jobCompanyCache.put(resume.getJobId(), scopedCompanyId);
            }

            return companyId.equals(scopedCompanyId);
        }).collect(Collectors.toList());

        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        int fromIndex = pageNumber * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, scopedResumes.size());

        List<Resume> pagedResumes = fromIndex >= scopedResumes.size()
                ? List.of()
                : scopedResumes.subList(fromIndex, toIndex);

        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();

        meta.setPage(pageNumber + 1);
        meta.setPageSize(pageSize);
        meta.setTotal(scopedResumes.size());
        meta.setPages(pageSize == 0 ? 0 : (int) Math.ceil((double) scopedResumes.size() / pageSize));

        result.setMeta(meta);
        result.setResult(pagedResumes.stream().map(this::getResume).collect(Collectors.toList()));

        return result;
    }

    public ResultPaginationDTO fetchAllResumeByUser(Pageable pageable) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            ResultPaginationDTO emptyResult = new ResultPaginationDTO();
            ResultPaginationDTO.Meta emptyMeta = new ResultPaginationDTO.Meta();
            emptyMeta.setPage(pageable.getPageNumber() + 1);
            emptyMeta.setPageSize(pageable.getPageSize());
            emptyMeta.setPages(0);
            emptyMeta.setTotal(0);
            emptyResult.setMeta(emptyMeta);
            emptyResult.setResult(List.of());
            return emptyResult;
        }

        Page<Resume> pageResume = this.resumeRepository.findByUserId(currentUserId, pageable);

        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();

        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());

        meta.setPages(pageResume.getTotalPages());
        meta.setTotal(pageResume.getTotalElements());

        result.setMeta(meta);

        List<ResFetchResumeDTO> listResume = pageResume.getContent()
                .stream().map(item -> this.getResume(item))
                .collect(Collectors.toList());

        result.setResult(listResume);
        return result;
    }

}
