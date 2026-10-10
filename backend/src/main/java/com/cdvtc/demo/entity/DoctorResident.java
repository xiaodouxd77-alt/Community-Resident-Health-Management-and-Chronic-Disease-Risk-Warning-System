package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 医生-居民授权关系。一名居民至多归属一名责任医生。
 */
@Data
@TableName("doctor_resident")
public class DoctorResident {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long doctorId;

    private Long residentId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
