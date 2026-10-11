package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 随访计划。医生为居民制定的一次随访安排，可关联慢病档案与异常预警（均可为空）。
 */
@Data
@TableName("followup_plan")
public class FollowupPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long residentId;

    private Long doctorId;

    private Long chronicDiseaseRecordId;

    private Long alertId;

    private String planTitle;

    private String followupPurpose;

    private LocalDateTime scheduledAt;

    /** PHONE / OFFLINE / ONLINE / OTHER */
    private String followupMethod;

    /** PLANNED / IN_PROGRESS / COMPLETED / CANCELLED */
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
