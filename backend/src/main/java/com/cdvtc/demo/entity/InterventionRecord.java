package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 干预执行记录。同一方案多条追加保存；
 * operator_role 区分医生执行记录（DOCTOR）与居民反馈（RESIDENT），互不覆盖。
 */
@Data
@TableName("intervention_record")
public class InterventionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long interventionPlanId;

    private Long residentId;

    private Long operatorId;

    /** DOCTOR / RESIDENT */
    private String operatorRole;

    private LocalDateTime executedAt;

    private String executionContent;

    private String residentFeedback;

    /** COMPLETED / PARTIAL / NOT_COMPLETED / NEEDS_ADJUSTMENT */
    private String executionStatus;

    private String effectSummary;

    private String nextAction;

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
