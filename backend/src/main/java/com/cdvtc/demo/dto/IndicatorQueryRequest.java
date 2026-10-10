package com.cdvtc.demo.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/** 指标历史查询参数。 */
@Data
public class IndicatorQueryRequest {

    private Integer page = 1;
    private Integer size = 10;

    /** 指标类型，为空表示全部 */
    private String indicatorType;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
