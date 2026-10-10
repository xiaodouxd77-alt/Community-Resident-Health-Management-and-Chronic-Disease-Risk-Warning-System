package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("health_indicator_record")
public class HealthIndicatorRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long residentId;

    private String indicatorType;

    /** 主数值：收缩压 / 血糖 / 体重 / 心率 */
    private BigDecimal numericValue;

    /** 第二数值：舒张压（仅血压使用） */
    private BigDecimal secondaryValue;

    private String unit;

    private LocalDateTime measuredAt;

    private String measurementContext;

    private String source;

    private String remark;

    /** 客户端幂等键，防止重复提交产生重复记录 */
    private String requestId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
