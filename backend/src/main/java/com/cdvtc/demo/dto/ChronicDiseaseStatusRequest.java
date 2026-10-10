package com.cdvtc.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 管理状态变更：ACTIVE / SUSPENDED / CLOSED。 */
@Data
public class ChronicDiseaseStatusRequest {

    @NotBlank(message = "请选择管理状态")
    private String managementStatus;

    @Size(max = 500, message = "变更说明长度不能超过500")
    private String comment;
}
