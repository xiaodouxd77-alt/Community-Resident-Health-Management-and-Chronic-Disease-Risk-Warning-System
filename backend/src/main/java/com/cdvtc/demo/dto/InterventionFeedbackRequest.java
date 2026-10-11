package com.cdvtc.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 居民提交干预执行反馈。仅可写反馈内容与自报执行情况，
 * 不能填写医生的执行内容 / 效果摘要 / 内部备注，也不会覆盖医生记录。
 */
@Data
public class InterventionFeedbackRequest {

    @NotBlank(message = "请填写反馈内容")
    @Size(max = 500, message = "反馈内容长度不能超过500")
    private String residentFeedback;

    /** 居民自报执行情况，可为空 */
    private String executionStatus;

    /** 反馈时间，可为空，缺省取当前时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime executedAt;
}
