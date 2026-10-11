package com.cdvtc.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/** 填写随访记录。可选同时完成计划、并按建议下次随访时间生成下一次计划。 */
@Data
public class FollowupRecordCreateRequest {

    @NotNull(message = "请选择实际随访时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime followedUpAt;

    @NotBlank(message = "请选择实际随访方式")
    private String followupMethod;

    @Size(max = 500, message = "居民反馈长度不能超过500")
    private String residentFeedback;

    @Size(max = 500, message = "健康情况摘要长度不能超过500")
    private String healthSummary;

    @Size(max = 500, message = "指标变化摘要长度不能超过500")
    private String indicatorSummary;

    @Size(max = 500, message = "干预建议长度不能超过500")
    private String interventionAdvice;

    @Size(max = 500, message = "随访结果长度不能超过500")
    private String followupResult;

    /** 建议下次随访时间，可为空 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextFollowupAt;

    private Boolean residentVisible;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

    /** 提交记录后是否同时将计划置为已完成 */
    private Boolean complete;

    /** 是否按 nextFollowupAt 自动创建下一次随访计划 */
    private Boolean createNextPlan;
}
