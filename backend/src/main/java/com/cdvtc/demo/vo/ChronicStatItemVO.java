package com.cdvtc.demo.vo;

import lombok.Data;

/** 慢病类型分布项（仅统计已核实档案）。 */
@Data
public class ChronicStatItemVO {

    private String diseaseType;
    private String diseaseTypeLabel;

    /** 去重人数 */
    private Long residentCount;

    /** 档案记录数 */
    private Long recordCount;

    /** 占已核实档案总数的比例（0~1，保留 4 位） */
    private Double recordRatio;
}
