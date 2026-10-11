package com.cdvtc.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** 创建健康干预方案。关联项（慢病/预警/随访计划）均可为空。 */
@Data
public class InterventionPlanCreateRequest {

    @NotNull(message = "请选择居民")
    private Long residentId;

    /** 关联慢病档案，可为空 */
    private Long chronicDiseaseRecordId;

    /** 关联异常预警，可为空 */
    private Long alertId;

    /** 关联随访计划，可为空 */
    private Long followupPlanId;

    @NotBlank(message = "请输入干预方案名称")
    @Size(max = 100, message = "方案名称长度不能超过100")
    private String planName;

    @NotBlank(message = "请选择干预类型")
    private String interventionType;

    @Size(max = 500, message = "干预目标长度不能超过500")
    private String interventionGoal;

    @Size(max = 1000, message = "干预措施长度不能超过1000")
    private String interventionContent;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /** 默认允许居民查看 */
    private Boolean residentVisible;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
