package com.cdvtc.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ChronicDiseaseCreateRequest {

    @NotNull(message = "请选择居民")
    private Long residentId;

    @NotBlank(message = "请选择慢病类型")
    private String diseaseType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate diagnosisDate;

    @Size(max = 500, message = "诊断依据长度不能超过500")
    private String diagnosisBasis;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
