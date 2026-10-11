package com.cdvtc.demo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 高风险居民筛查结果行。
 * 风险等级来自最近一次风险评估，未处理预警与已核实慢病为独立维度，不互相推导。
 */
@Data
public class HighRiskResidentVO {

    private Long residentId;
    private String residentNo;
    private String realName;
    private String gender;
    private String genderLabel;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private Integer age;
    private String communityName;

    /** 负责医生 */
    private Long doctorId;
    private String doctorName;

    /** 最近一次风险评估 */
    private String latestRiskLevel;
    private String latestRiskLevelLabel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime latestRiskAssessedAt;

    /** 预警统计：未处理=PENDING+PROCESSING */
    private Long unhandledAlertCount;
    private Long totalAlertCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime latestAlertAt;

    /** 已核实慢病档案数 */
    private Long confirmedChronicCount;
    private List<ChronicSummaryVO> chronicSummaries;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMeasuredAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastFollowupAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastInterventionAt;
}
