package com.cdvtc.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/** 修改未完成的随访计划：字段为空表示不修改。 */
@Data
public class FollowupPlanUpdateRequest {

    @Size(max = 100, message = "计划标题长度不能超过100")
    private String planTitle;

    @Size(max = 500, message = "随访目的长度不能超过500")
    private String followupPurpose;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledAt;

    private String followupMethod;

    private Boolean residentVisible;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
