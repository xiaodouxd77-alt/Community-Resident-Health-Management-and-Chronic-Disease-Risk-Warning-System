package com.cdvtc.demo.vo;

import lombok.Data;

/** 指标录入结果：记录、评估结论、是否产生预警。 */
@Data
public class IndicatorSubmitResultVO {

    /** 指标记录是否已保存（本流程为同一事务，成功时必为 true） */
    private Boolean recordSaved;

    /** 风险评估是否完成 */
    private Boolean assessmentCompleted;

    /** 是否产生预警 */
    private Boolean alertCreated;

    private String message;

    private IndicatorRecordVO record;

    private AssessmentVO assessment;

    private AlertVO alert;
}
