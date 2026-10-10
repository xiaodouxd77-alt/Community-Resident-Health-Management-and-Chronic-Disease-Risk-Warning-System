package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 居民分组（管理标签，非诊断结论）。 */
@Data
@TableName("resident_group")
public class ResidentGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String groupName;

    private String groupCode;

    private String description;

    private Long communityId;

    private Long managerDoctorId;

    private Long createdBy;

    /** ACTIVE / DISABLED */
    private String status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
