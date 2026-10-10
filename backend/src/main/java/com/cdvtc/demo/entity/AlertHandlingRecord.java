package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("alert_handling_record")
public class AlertHandlingRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long alertId;

    private Long doctorId;

    /** START / PROGRESS / RESOLVE */
    private String actionType;

    private String handlingComment;

    private Integer followupRequired;

    private LocalDateTime nextFollowupAt;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
