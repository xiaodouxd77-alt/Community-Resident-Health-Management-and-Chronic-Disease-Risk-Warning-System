package com.cdvtc.demo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class IndicatorRecordVO {

    private Long id;
    private String indicatorType;
    private String indicatorTypeLabel;
    private BigDecimal numericValue;
    private BigDecimal secondaryValue;
    private String unit;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime measuredAt;

    private String measurementContext;
    private String measurementContextLabel;
    private String source;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 关联的评估与预警信息，便于历史记录页展示关联关系 */
    private Long assessmentId;
    private String assessmentRiskLevel;
    private String assessmentRiskLevelLabel;
    private Long alertId;
    private String alertStatus;
}
