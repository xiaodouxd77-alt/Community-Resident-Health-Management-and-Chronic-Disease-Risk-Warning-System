package com.cdvtc.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 居民可自行维护的基础资料。不包含医生诊断、审核结果、管理归属、风险评估等业务字段。
 */
@Data
public class HealthProfileUpdateRequest {

    @Size(max = 50, message = "姓名长度不能超过50")
    private String realName;

    private String gender;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @DecimalMin(value = "30", message = "身高需在 30~250cm 之间")
    @DecimalMax(value = "250", message = "身高需在 30~250cm 之间")
    private BigDecimal height;

    @Size(max = 500, message = "既往病史长度不能超过500")
    private String pastHistory;

    @Size(max = 500, message = "家族病史长度不能超过500")
    private String familyHistory;

    @Size(max = 500, message = "过敏史长度不能超过500")
    private String allergyHistory;

    @Size(max = 500, message = "健康备注长度不能超过500")
    private String healthRemark;

    private Long communityId;
}
