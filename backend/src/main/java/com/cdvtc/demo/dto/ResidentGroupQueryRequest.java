package com.cdvtc.demo.dto;

import lombok.Data;

/** 医生端居民分组检索参数。 */
@Data
public class ResidentGroupQueryRequest {

    private Integer page = 1;
    private Integer size = 10;

    /** 分组名称或编码 */
    private String keyword;

    private Long communityId;

    private String status;

    private Long managerDoctorId;
}
