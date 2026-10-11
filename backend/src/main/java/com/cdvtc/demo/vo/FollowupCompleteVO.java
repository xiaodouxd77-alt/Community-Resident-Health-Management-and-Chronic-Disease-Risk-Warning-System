package com.cdvtc.demo.vo;

import lombok.Data;

/** 提交随访记录后的结果：本次记录 + 计划最新状态 + 可能生成的下一次计划。 */
@Data
public class FollowupCompleteVO {

    private FollowupRecordVO record;

    private FollowupPlanVO plan;

    /** 若按建议时间自动创建了下一次计划，则为该计划；否则为 null */
    private FollowupPlanVO nextPlan;
}
