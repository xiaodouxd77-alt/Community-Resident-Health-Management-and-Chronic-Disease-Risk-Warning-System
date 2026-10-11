package com.cdvtc.demo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InterventionPlanVO {

    private Long id;

    private Long residentId;
    private String residentNo;
    private String residentName;
    private String gender;
    private String genderLabel;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private String communityName;
    private String phone;

    private Long doctorId;
    private String doctorName;

    private Long chronicDiseaseRecordId;
    private String chronicDiseaseType;
    private String chronicDiseaseTypeLabel;

    private Long alertId;
    private String alertTitle;
    private String alertRiskLevel;
    private String alertRiskLevelLabel;

    private Long followupPlanId;
    private String followupPlanTitle;

    private String planName;
    private String interventionType;
    private String interventionTypeLabel;
    private String interventionGoal;
    private String interventionContent;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String planStatus;
    private String planStatusLabel;

    private Boolean residentVisible;

    private String remark;

    private Long recordCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastExecutedAt;

    private String latestExecutionStatus;
    private String latestExecutionStatusLabel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
