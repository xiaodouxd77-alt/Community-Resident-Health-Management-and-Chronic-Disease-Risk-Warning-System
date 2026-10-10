package com.cdvtc.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** 慢病档案允许修改的管理信息（不含慢病类型、审核结果、管理状态）。 */
@Data
public class ChronicDiseaseUpdateRequest {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate diagnosisDate;

    @Size(max = 500, message = "诊断依据长度不能超过500")
    private String diagnosisBasis;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
