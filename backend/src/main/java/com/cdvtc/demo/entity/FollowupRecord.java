package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 随访记录。同一计划可有多条，追加式保留每次随访历史。
 */
@Data
@TableName("followup_record")
public class FollowupRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private Long residentId;

    private Long doctorId;

    private LocalDateTime followedUpAt;

    /** PHONE / OFFLINE / ONLINE / OTHER */
    private String followupMethod;

    private String residentFeedback;

    private String healthSummary;

    private String indicatorSummary;

    private String interventionAdvice;

    private String followupResult;

    private LocalDateTime nextFollowupAt;

    /** 0-否 1-是 */
    private Integer residentVisible;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
