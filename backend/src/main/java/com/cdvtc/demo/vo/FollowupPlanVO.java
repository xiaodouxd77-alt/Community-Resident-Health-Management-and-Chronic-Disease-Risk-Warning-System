package com.cdvtc.demo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FollowupPlanVO {

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

    private String planTitle;
    private String followupPurpose;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledAt;

    private String followupMethod;
    private String followupMethodLabel;

    private String planStatus;
    private String planStatusLabel;

    /** 动态计算的逾期标记：未结束且计划时间已过 */
    private Boolean overdue;
    private String overdueLabel;

    private Boolean residentVisible;

    private String remark;

    private Long recordCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastFollowedUpAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
