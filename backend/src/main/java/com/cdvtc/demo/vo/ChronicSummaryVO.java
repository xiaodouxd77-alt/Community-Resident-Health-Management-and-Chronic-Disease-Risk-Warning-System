package com.cdvtc.demo.vo;

import lombok.Data;

/** 居民慢病摘要（仅包含已核实档案，用于风险筛查结果展示）。 */
@Data
public class ChronicSummaryVO {

    private Long recordId;
    private String diseaseType;
    private String diseaseTypeLabel;
    private String diagnosisStatus;
    private String diagnosisStatusLabel;
    private String managementStatus;
    private String managementStatusLabel;
}
