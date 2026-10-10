package com.cdvtc.demo.dto;

import lombok.Data;

/** 医生端慢病档案检索参数。 */
@Data
public class ChronicDiseaseQueryRequest {

    private Integer page = 1;
    private Integer size = 10;

    /** 居民姓名或编号 */
    private String keyword;

    private String diseaseType;

    private String managementStatus;

    private String diagnosisStatus;

    private Long managingDoctorId;
}
