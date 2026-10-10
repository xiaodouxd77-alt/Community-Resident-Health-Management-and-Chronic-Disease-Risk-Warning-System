package com.cdvtc.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 诊断资料审核：仅允许 CONFIRMED / REJECTED。 */
@Data
public class ChronicDiseaseReviewRequest {

    @NotBlank(message = "请选择审核结果")
    private String diagnosisStatus;

    @Size(max = 500, message = "审核意见长度不能超过500")
    private String comment;
}
