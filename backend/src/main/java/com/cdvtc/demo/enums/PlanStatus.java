package com.cdvtc.demo.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 随访计划状态。
 * 逾期不是独立状态，而是「未结束的计划且计划时间已过」的动态判定，
 * 避免仅因时间流逝产生数据库状态不一致。
 */
public enum PlanStatus {

    PLANNED("待执行"),
    IN_PROGRESS("进行中"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String label;

    PlanStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 是否为已结束状态（不可再修改、取消或执行）。 */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /** 是否为待办状态（待执行/进行中，可被随访）。 */
    public boolean isOpen() {
        return this == PLANNED || this == IN_PROGRESS;
    }

    public static Optional<PlanStatus> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    public static String labelOf(String code) {
        return fromCode(code).map(PlanStatus::getLabel).orElse(code);
    }
}
