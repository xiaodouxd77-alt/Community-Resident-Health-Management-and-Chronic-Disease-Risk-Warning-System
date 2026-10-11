package com.cdvtc.demo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/** 干预方案阶段性效果。数据来自执行记录，不新增存储。 */
@Data
public class InterventionEffectVO {

    private Long planId;
    private String planName;

    private Long recordCount;
    private Long doctorRecordCount;
    private Long residentFeedbackCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastExecutedAt;

    private String latestExecutionStatus;
    private String latestExecutionStatusLabel;

    private String latestEffectSummary;
    private String latestNextAction;

    /** 最新执行情况为「未执行 / 需要调整」，或尚无执行记录 */
    private Boolean needsAttention;
}
