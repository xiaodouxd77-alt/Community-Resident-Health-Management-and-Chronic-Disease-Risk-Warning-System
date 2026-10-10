package com.cdvtc.demo.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/** 预警列表查询参数。 */
@Data
public class AlertQueryRequest {

    private Integer page = 1;
    private Integer size = 10;

    /** PENDING / PROCESSING / RESOLVED */
    private String status;

    /** LOW / MEDIUM / HIGH / CRITICAL */
    private String riskLevel;

    /** 医生端按居民过滤（会再次校验是否在其授权范围） */
    private Long residentId;

    /** 关键词：居民编号或姓名 */
    private String keyword;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
