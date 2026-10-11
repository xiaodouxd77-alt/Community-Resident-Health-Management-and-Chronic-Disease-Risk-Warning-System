package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 社区公告。管理员维护，居民只读已发布且处于有效期内的公告。
 */
@Data
@TableName("system_notice")
public class SystemNotice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private Long publisherId;

    /** DRAFT / PUBLISHED / OFFLINE */
    private String publishStatus;

    private LocalDateTime publishedAt;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveUntil;

    /** 0-否 1-是 */
    private Integer isTop;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
