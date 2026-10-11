package com.cdvtc.demo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FollowupRecordVO {

    private Long id;

    private Long planId;
    private String planTitle;

    private Long residentId;
    private String residentNo;
    private String residentName;

    private Long doctorId;
    private String doctorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime followedUpAt;

    private String followupMethod;
    private String followupMethodLabel;

    private String residentFeedback;
    private String healthSummary;
    private String indicatorSummary;
    private String interventionAdvice;
    private String followupResult;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextFollowupAt;

    private Boolean residentVisible;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
