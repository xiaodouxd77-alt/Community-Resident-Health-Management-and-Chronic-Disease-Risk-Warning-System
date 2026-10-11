package com.cdvtc.demo.vo;

import lombok.Data;

/** 预警趋势点（按时间分桶）。 */
@Data
public class AlertTrendPointVO {

    /** 分桶标签：按天 yyyy-MM-dd，按月 yyyy-MM */
    private String bucket;

    private Long pendingCount;
    private Long processingCount;
    private Long resolvedCount;
    private Long totalCount;
}
