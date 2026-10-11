package com.cdvtc.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/** 医生端随访计划检索参数。 */
@Data
public class FollowupPlanQueryRequest {

    private Integer page = 1;
    private Integer size = 10;

    /** 居民姓名或编号 */
    private String keyword;

    private Long residentId;

    private String planStatus;

    private String followupMethod;

    /** 计划随访时间范围（含） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 仅看逾期（未结束且计划时间已过） */
    private Boolean overdueOnly;
}
