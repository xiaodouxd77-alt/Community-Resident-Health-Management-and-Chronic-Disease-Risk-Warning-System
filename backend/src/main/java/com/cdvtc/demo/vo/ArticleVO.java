package com.cdvtc.demo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleVO {

    private Long id;

    private String title;

    private String summary;

    private String content;

    private String category;

    private String categoryLabel;

    private String coverUrl;

    private Long authorId;

    private String authorName;

    private String publishStatus;

    private String publishStatusLabel;

    private LocalDateTime publishedAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
