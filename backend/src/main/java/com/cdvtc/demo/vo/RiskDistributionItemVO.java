package com.cdvtc.demo.vo;

import lombok.Data;

/** 风险等级分布项。 */
@Data
public class RiskDistributionItemVO {

    private String riskLevel;
    private String riskLevelLabel;
    private Long count;
}
