package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("community")
public class Community {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String communityCode;

    private String communityName;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
