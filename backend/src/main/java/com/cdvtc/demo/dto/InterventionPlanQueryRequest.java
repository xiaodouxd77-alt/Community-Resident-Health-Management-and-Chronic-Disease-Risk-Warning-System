package com.cdvtc.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/** 医生端干预方案检索参数。 */
@Data
public class InterventionPlanQueryRequest {

    private Integer page = 1;
    private Integer size = 10;

    /** 居民姓名或编号 */
    private String keyword;

    private Long residentId;

    private String interventionType;

    private String planStatus;

    /** 开始日期范围（含） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDateFrom;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDateTo;
}
