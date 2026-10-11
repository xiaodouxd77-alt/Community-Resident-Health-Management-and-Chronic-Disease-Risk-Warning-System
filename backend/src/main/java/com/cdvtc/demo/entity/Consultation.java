package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 在线健康咨询。doctor_id 为空表示尚未受理。
 */
@Data
@TableName("consultation")
public class Consultation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long residentId;

    private Long doctorId;

    private String title;

    /** HEALTH_INDICATOR / CHRONIC_MANAGEMENT / LIFESTYLE / OTHER */
    private String category;

    private String description;

    /** PENDING / PROCESSING / RESOLVED / CLOSED */
    private String status;

    private LocalDateTime acceptedAt;

    private LocalDateTime resolvedAt;

    private LocalDateTime closedAt;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
