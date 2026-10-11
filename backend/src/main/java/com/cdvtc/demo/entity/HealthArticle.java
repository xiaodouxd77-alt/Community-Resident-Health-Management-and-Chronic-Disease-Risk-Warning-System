package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 健康知识文章。管理员维护，居民只读已发布。
 */
@Data
@TableName("health_article")
public class HealthArticle {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String summary;

    private String content;

    /** CHRONIC / DIET / EXERCISE / LIFESTYLE / ELDERLY / OTHER */
    private String category;

    private String coverUrl;

    private Long authorId;

    /** DRAFT / PUBLISHED / OFFLINE */
    private String publishStatus;

    private LocalDateTime publishedAt;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
