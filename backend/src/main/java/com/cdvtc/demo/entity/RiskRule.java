package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("risk_rule")
public class RiskRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleCode;

    private String ruleName;

    private String indicatorType;

    /** 适用测量场景，NULL 表示不限 */
    private String measurementContext;

    /** PRIMARY-主数值 SECONDARY-第二数值（血压舒张压） */
    private String valueField;

    private String operator;

    private BigDecimal thresholdValue;

    private String riskLevel;

    private String alertType;

    private String description;

    private String ruleVersion;

    private Integer enabled;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
