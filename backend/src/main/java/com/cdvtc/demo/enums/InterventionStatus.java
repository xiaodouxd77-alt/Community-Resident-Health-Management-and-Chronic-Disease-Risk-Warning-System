package com.cdvtc.demo.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 干预方案状态。状态流转集中在 Service 层校验：
 *   DRAFT     → ACTIVE / CANCELLED
 *   ACTIVE    → SUSPENDED / COMPLETED / CANCELLED
 *   SUSPENDED → ACTIVE（恢复）/ COMPLETED / CANCELLED
 *   COMPLETED / CANCELLED 为终态，不可再变更。
 */
public enum InterventionStatus {

    DRAFT("草稿"),
    ACTIVE("执行中"),
    COMPLETED("已完成"),
    SUSPENDED("已暂停"),
    CANCELLED("已取消");

    private final String label;

    InterventionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 终态：不可再启动、暂停、完成或取消。 */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /** 可执行（可新增执行记录）的状态。 */
    public boolean isExecutable() {
        return this == ACTIVE || this == SUSPENDED;
    }

    public static Optional<InterventionStatus> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    public static String labelOf(String code) {
        return fromCode(code).map(InterventionStatus::getLabel).orElse(code);
    }
}
