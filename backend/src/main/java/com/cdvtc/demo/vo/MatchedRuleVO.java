package com.cdvtc.demo.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 评估命中的单条规则快照，用于向用户解释评估依据。 */
@Data
public class MatchedRuleVO {

    private String ruleCode;
    private String ruleName;
    private String valueField;
    private String valueFieldLabel;
    private String operator;
    private BigDecimal thresholdValue;
    private BigDecimal actualValue;
    private String riskLevel;
    private String riskLevelLabel;
    private String measurementContext;
    private String description;
    private String ruleVersion;
    private String alertType;
}
