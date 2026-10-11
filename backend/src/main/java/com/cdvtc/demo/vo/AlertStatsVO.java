package com.cdvtc.demo.vo;

import lombok.Data;

import java.util.List;

/** 预警统计（处理率明确给出分子/分母）。 */
@Data
public class AlertStatsVO {

    /** 预警总数（分母） */
    private Long totalCount;

    private Long pendingCount;
    private Long processingCount;

    /** 已完成数（分子） */
    private Long resolvedCount;

    /** 未处理数 = pending + processing */
    private Long unhandledCount;

    /** 处理率 = resolvedCount / totalCount（0~1，保留 4 位；分母为 0 时为 0） */
    private Double handlingRate;

    /** 按风险等级分布 */
    private List<RiskDistributionItemVO> byRiskLevel;
}
