package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 健康干预方案。医生为居民制定的健康管理方案，
 * 可关联慢病档案 / 异常预警 / 随访计划（均可为空）。
 */
@Data
@TableName("intervention_plan")
public class InterventionPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long residentId;

    private Long doctorId;

    private Long chronicDiseaseRecordId;

    private Long alertId;

    private Long followupPlanId;

    private String planName;

    /** LIFESTYLE / DIET / EXERCISE / MONITORING / FOLLOWUP / OTHER */
    private String interventionType;

    private String interventionGoal;

    private String interventionContent;

    private LocalDate startDate;

    private LocalDate endDate;

    /** DRAFT / ACTIVE / COMPLETED / SUSPENDED / CANCELLED */
    private String planStatus;

    /** 0-否 1-是 */
    private Integer residentVisible;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
