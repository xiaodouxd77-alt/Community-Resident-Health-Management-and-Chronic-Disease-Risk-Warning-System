package com.cdvtc.demo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AssessmentVO {

    private Long id;
    private String assessmentCode;
    private Long indicatorRecordId;
    private Long residentId;
    private String indicatorType;
    private String indicatorTypeLabel;
    private String riskLevel;
    private String riskLevelLabel;
    private String assessmentSummary;
    private String ruleVersion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime assessedAt;

    /** 命中的规则（评估依据） */
    private List<MatchedRuleVO> matchedRules;

    private Long alertId;
    private String alertNo;
    private String alertStatus;
}
