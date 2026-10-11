package com.cdvtc.demo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InterventionRecordVO {

    private Long id;

    private Long interventionPlanId;
    private String planName;

    private Long residentId;
    private String residentNo;
    private String residentName;

    private Long operatorId;
    private String operatorName;
    private String operatorRole;
    private String operatorRoleLabel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime executedAt;

    private String executionContent;
    private String residentFeedback;

    private String executionStatus;
    private String executionStatusLabel;

    private String effectSummary;
    private String nextAction;

    private Boolean residentVisible;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
