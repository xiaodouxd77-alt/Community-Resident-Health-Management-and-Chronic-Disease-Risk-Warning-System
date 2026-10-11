package com.cdvtc.demo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 高风险居民详情：在筛选项基础上补充最近评估、慢病档案、预警与指标。 */
@Data
public class HighRiskResidentDetailVO {

    private HealthProfileVO resident;

    private String latestRiskLevel;
    private String latestRiskLevelLabel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime latestRiskAssessedAt;

    private AssessmentVO latestAssessment;

    private List<ChronicDiseaseVO> chronicDiseases;

    private Long unhandledAlertCount;
    private Long totalAlertCount;
    private List<AlertVO> alerts;

    private List<IndicatorRecordVO> recentIndicators;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMeasuredAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastFollowupAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastInterventionAt;
}
