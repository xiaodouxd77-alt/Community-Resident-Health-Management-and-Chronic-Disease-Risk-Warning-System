package com.cdvtc.demo.vo;

import lombok.Data;

import java.util.List;

/** 预警详情：预警本体 + 评估依据 + 处理历史 + 居民信息 + 相关历史指标。 */
@Data
public class AlertDetailVO {

    private AlertVO alert;
    private AssessmentVO assessment;
    private ResidentBriefVO resident;
    private List<HandlingRecordVO> handlingRecords;
    private List<IndicatorRecordVO> recentIndicators;
}
