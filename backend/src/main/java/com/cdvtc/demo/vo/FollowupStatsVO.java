package com.cdvtc.demo.vo;

import lombok.Data;

/** 医生随访工作台统计（均来自真实数据）。 */
@Data
public class FollowupStatsVO {

    private Long plannedCount;
    private Long inProgressCount;
    private Long completedCount;
    private Long cancelledCount;
    private Long overdueCount;
}
