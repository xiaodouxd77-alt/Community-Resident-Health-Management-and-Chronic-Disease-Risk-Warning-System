package com.cdvtc.demo.dto;

import lombok.Data;

/** 医生端居民检索参数。 */
@Data
public class ResidentQueryRequest {

    private Integer page = 1;
    private Integer size = 10;

    /** 姓名或居民编号关键词 */
    private String keyword;

    private Long communityId;
}
