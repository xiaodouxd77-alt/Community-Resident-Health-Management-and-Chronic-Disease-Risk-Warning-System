package com.cdvtc.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/** 医生填写干预执行记录。执行情况、居民反馈、阶段性效果均为可选项。 */
@Data
public class InterventionRecordCreateRequest {

    @NotNull(message = "请选择实际执行时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime executedAt;

    @Size(max = 1000, message = "执行内容长度不能超过1000")
    private String executionContent;

    @Size(max = 500, message = "居民反馈长度不能超过500")
    private String residentFeedback;

    private String executionStatus;

    @Size(max = 500, message = "阶段性效果长度不能超过500")
    private String effectSummary;

    @Size(max = 500, message = "后续计划长度不能超过500")
    private String nextAction;

    private Boolean residentVisible;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
