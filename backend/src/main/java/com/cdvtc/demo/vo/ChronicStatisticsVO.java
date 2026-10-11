package com.cdvtc.demo.vo;

import lombok.Data;

import java.util.List;

/** 慢病类型分布统计。 */
@Data
public class ChronicStatisticsVO {

    /** 已核实档案总数（分母） */
    private Long confirmedRecordCount;

    /** 已核实档案涉及的去重居民数 */
    private Long confirmedResidentCount;

    /** 按类型分布；各类型人数相加可能大于去重总人数（一人可患多病） */
    private List<ChronicStatItemVO> items;
}
