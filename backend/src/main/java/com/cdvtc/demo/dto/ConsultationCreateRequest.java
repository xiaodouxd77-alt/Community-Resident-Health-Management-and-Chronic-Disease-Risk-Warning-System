package com.cdvtc.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConsultationCreateRequest {

    @NotBlank(message = "请输入咨询标题")
    @Size(max = 100, message = "咨询标题长度不能超过100")
    private String title;

    @NotBlank(message = "请选择咨询分类")
    private String category;

    @NotBlank(message = "请填写问题描述")
    @Size(max = 2000, message = "问题描述长度不能超过2000")
    private String description;
}
