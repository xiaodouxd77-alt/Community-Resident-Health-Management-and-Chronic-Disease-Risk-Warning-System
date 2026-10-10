package com.cdvtc.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class IndicatorCreateRequest {

    @NotBlank(message = "指标类型不能为空")
    private String indicatorType;

    @NotNull(message = "指标数值不能为空")
    private BigDecimal numericValue;

    /** 血压舒张压；仅血压填写 */
    private BigDecimal secondaryValue;

    @NotBlank(message = "单位不能为空")
    private String unit;

    @NotNull(message = "测量时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime measuredAt;

    /** 测量场景，血糖必填 */
    private String measurementContext;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

    /** 客户端幂等键，防止重复提交 */
    @NotBlank(message = "请求标识不能为空")
    @Size(max = 64, message = "请求标识过长")
    private String requestId;
}
