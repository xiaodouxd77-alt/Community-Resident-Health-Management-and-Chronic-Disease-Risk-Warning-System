package com.cdvtc.demo.dto;

import lombok.Data;

@Data
public class NoticeQueryRequest {

    private Integer page;

    private Integer size;

    /** 标题关键词，可为空 */
    private String keyword;

    /** 发布状态筛选，可为空 */
    private String status;
}
