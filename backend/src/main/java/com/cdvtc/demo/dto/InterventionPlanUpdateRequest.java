package com.cdvtc.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** 修改干预方案：字段为空表示不修改。终态方案不可修改。 */
@Data
public class InterventionPlanUpdateRequest {

    private Long chronicDiseaseRecordId;

    private Long alertId;

    private Long followupPlanId;

    @Size(max = 100, message = "方案名称长度不能超过100")
    private String planName;

    private String interventionType;

    @Size(max = 500, message = "干预目标长度不能超过500")
    private String interventionGoal;

    @Size(max = 1000, message = "干预措施长度不能超过1000")
    private String interventionContent;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Boolean residentVisible;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
