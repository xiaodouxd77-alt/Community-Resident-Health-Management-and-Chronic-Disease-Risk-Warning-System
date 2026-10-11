package com.cdvtc.demo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeVO {

    private Long id;

    private String title;

    private String content;

    private Long publisherId;

    private String publisherName;

    private String publishStatus;

    private String publishStatusLabel;

    private LocalDateTime publishedAt;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveUntil;

    private Boolean isTop;

    /** 是否已过有效期（管理端展示用） */
    private Boolean expired;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
