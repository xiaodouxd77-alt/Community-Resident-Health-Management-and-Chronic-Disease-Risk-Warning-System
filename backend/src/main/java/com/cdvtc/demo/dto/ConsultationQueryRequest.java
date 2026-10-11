package com.cdvtc.demo.dto;

import lombok.Data;

@Data
public class ConsultationQueryRequest {

    private Integer page;

    private Integer size;

    /** 咨询状态筛选，可为空 */
    private String status;

    /** 标题/问题描述关键词，可为空 */
    private String keyword;
}
