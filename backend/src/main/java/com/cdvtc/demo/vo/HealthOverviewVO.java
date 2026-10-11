package com.cdvtc.demo.vo;

import lombok.Data;

/**
 * 健康概览统计。人数口径与记录数口径分开，避免重复计数被误读。
 * 全部字段来自 SQL 聚合，均以当前数据范围为边界（医生=管理范围内，管理员=全库）。
 */
@Data
public class HealthOverviewVO {

    /** 管理范围内居民总数（health_profile 数） */
    private Long residentCount;

    /** 健康档案已核实数（医生核实过基础资料） */
    private Long verifiedProfileCount;

    /** 有已核实慢病档案的居民数（去重人数） */
    private Long chronicConfirmedResidentCount;

    /** 已核实慢病档案数（记录数，可多于人数） */
    private Long chronicConfirmedRecordCount;

    /** 待核实慢病档案数（记录数） */
    private Long chronicPendingReviewRecordCount;

    /** 存在未处理预警的居民数（去重人数） */
    private Long unhandledAlertResidentCount;

    /** 未处理预警数（记录数，未处理=PENDING+PROCESSING） */
    private Long unhandledAlertCount;

    /** 高风险居民数（最近一次评估为高风险/严重风险，去重人数） */
    private Long highRiskResidentCount;

    /** 风险评估总数（记录数） */
    private Long totalAssessmentCount;

    /** 预警总数（记录数） */
    private Long totalAlertCount;

    /** 已处理完成预警数（记录数） */
    private Long resolvedAlertCount;
}
