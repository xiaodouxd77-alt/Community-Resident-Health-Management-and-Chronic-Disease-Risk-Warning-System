package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 居民分组成员关联（独立关联表，支持多对多）。 */
@Data
@TableName("resident_group_member")
public class ResidentGroupMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private Long residentId;

    private Long addedBy;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
