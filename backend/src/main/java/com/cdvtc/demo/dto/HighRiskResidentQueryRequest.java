package com.cdvtc.demo.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 高风险居民筛查查询参数。
 * 风险等级、未处理预警、已核实慢病是三个相互独立的维度，不做等价替换。
 */
@Data
public class HighRiskResidentQueryRequest {

    private Integer page = 1;
    private Integer size = 10;

    /** 居民姓名或编号 */
    private String keyword;

    /** 最近一次风险评估等级 LOW / MEDIUM / HIGH / CRITICAL */
    private String riskLevel;

    /** 是否存在未处理预警（PENDING / PROCESSING） */
    private Boolean hasUnhandledAlert;

    /** 存在指定处理状态的预警 PENDING / PROCESSING / RESOLVED */
    private String alertStatus;

    /** 已核实慢病类型 HYPERTENSION / DIABETES / OTHER */
    private String diseaseType;

    /** 是否已建有效慢病档案（已核实且未终止） */
    private Boolean hasChronic;

    /** 最近一次指标测量时间范围（含） */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMeasuredFrom;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMeasuredTo;

    /** 居民分组 */
    private Long groupId;
}
