package com.cdvtc.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FollowupPlanCreateRequest {

    @NotNull(message = "请选择居民")
    private Long residentId;

    /** 关联慢病档案，可为空 */
    private Long chronicDiseaseRecordId;

    /** 关联异常预警，可为空 */
    private Long alertId;

    @NotBlank(message = "请输入计划标题")
    @Size(max = 100, message = "计划标题长度不能超过100")
    private String planTitle;

    @Size(max = 500, message = "随访目的长度不能超过500")
    private String followupPurpose;

    @NotNull(message = "请选择计划随访时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledAt;

    @NotBlank(message = "请选择随访方式")
    private String followupMethod;

    /** 默认允许居民查看 */
    private Boolean residentVisible;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
