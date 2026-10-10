package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("alert_record")
public class AlertRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String alertNo;

    private Long residentId;

    private Long assessmentId;

    private Long indicatorRecordId;

    private String alertType;

    private String riskLevel;

    private String alertTitle;

    private String alertReason;

    /** PENDING / PROCESSING / RESOLVED */
    private String status;

    /** 负责医生 sys_user.id，NULL 表示待分配 */
    private Long assignedDoctorId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
