package vn.hoidanit.resumeservice.dto;

import java.time.Instant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import vn.hoidanit.resumeservice.util.constant.ResumeStateEnum;

@Getter
@Setter
public class ReqUpdateResumeDTO {
    @NotNull(message = "id khong duoc de trong")
    private Long id;

    private ResumeStateEnum status;

    @Min(value = 1, message = "rating phai tu 1 den 5")
    @Max(value = 5, message = "rating phai tu 1 den 5")
    private Integer rating;

    private String notes;

    private Instant interviewDateTime;

    private String interviewer;

    private String meetingType;

    private String meetingLink;

    private String meetingLocation;

    private String interviewNote;

    private String interviewResult;
}
