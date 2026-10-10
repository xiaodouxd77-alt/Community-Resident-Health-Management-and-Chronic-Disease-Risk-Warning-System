package com.cdvtc.demo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AlertVO {

    private Long id;
    private String alertNo;

    private Long residentId;
    private String residentNo;
    private String residentName;

    private Long assessmentId;
    private Long indicatorRecordId;

    private String indicatorType;
    private String indicatorTypeLabel;
    private BigDecimal numericValue;
    private BigDecimal secondaryValue;
    private String unit;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime measuredAt;

    private String alertType;
    private String riskLevel;
    private String riskLevelLabel;
    private String alertTitle;
    private String alertReason;
    private String status;
    private String statusLabel;
    private Long assignedDoctorId;
    private String assignedDoctorName;

    /** 最近一次处理意见，供列表与居民端展示 */
    private String latestHandlingComment;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime latestHandlingTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
