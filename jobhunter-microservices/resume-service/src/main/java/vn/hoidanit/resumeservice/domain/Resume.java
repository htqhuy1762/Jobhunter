package vn.hoidanit.resumeservice.domain;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import vn.hoidanit.resumeservice.util.SecurityUtil;
import vn.hoidanit.resumeservice.util.constant.ResumeStateEnum;

@Entity
@Table(name = "resumes")
@Getter
@Setter
public class Resume {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank(message = "email không được để trống")
    private String email;

    @NotBlank(message = "url không được để trống (upload cv chưa thành công)")
    private String url;

    @Enumerated(EnumType.STRING)
    private ResumeStateEnum status;

    private Integer rating;

    private String notes;

    private Long reviewedBy;

    private Instant reviewedAt;

    private Instant interviewDateTime;

    private String interviewer;

    private String meetingType;

    private String meetingLink;

    private String meetingLocation;

    private String interviewNote;

    private String interviewResult;

    private Instant createdAt;
    private Instant updatedAt;

    private String createdBy;
    private String updatedBy;

    // Relationships converted to service references
    // @ManyToOne
    // @JoinColumn(name = "user_id")
    // private User user;
    private Long userId; // Reference to Auth service

    // @ManyToOne
    // @JoinColumn(name = "job_id")
    // private Job job;
    private Long jobId; // Reference to Job service

    @PrePersist
    public void handleBeforeCreate() {
        this.createdBy = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        this.createdAt = Instant.now();
    }

    @PreUpdate
    public void handleBeforeUpdate() {
        this.updatedBy = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        this.updatedAt = Instant.now();
    }
}
