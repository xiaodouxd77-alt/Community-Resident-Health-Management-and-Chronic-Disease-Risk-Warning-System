package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 慢病档案变更历史（追加式，不覆盖）。 */
@Data
@TableName("chronic_disease_history")
public class ChronicDiseaseHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long chronicId;

    private Long residentId;

    /** CREATE / UPDATE / REVIEW / STATUS_CHANGE */
    private String action;

    private String detail;

    private Long operatorId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
