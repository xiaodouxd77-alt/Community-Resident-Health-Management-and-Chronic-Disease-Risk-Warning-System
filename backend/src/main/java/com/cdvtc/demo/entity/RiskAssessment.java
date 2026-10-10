package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("risk_assessment")
public class RiskAssessment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long residentId;

    private Long indicatorRecordId;

    private String assessmentCode;

    private String riskLevel;

    private String assessmentSummary;

    /** 触发规则与原因的结构化快照（JSON 字符串） */
    private String triggerDetails;

    private String ruleVersion;

    private LocalDateTime assessedAt;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
