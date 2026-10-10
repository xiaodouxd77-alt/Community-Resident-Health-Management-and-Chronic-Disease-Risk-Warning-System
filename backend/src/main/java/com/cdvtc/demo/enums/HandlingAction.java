package com.cdvtc.demo.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 预警处理动作及其目标状态。
 * START/PROGRESS 进入处理中，RESOLVE 完成处置。
 */
public enum HandlingAction {

    START("开始处理", AlertStatus.PROCESSING),
    PROGRESS("记录处理进展", AlertStatus.PROCESSING),
    RESOLVE("完成处理", AlertStatus.RESOLVED);

    private final String label;
    private final AlertStatus targetStatus;

    HandlingAction(String label, AlertStatus targetStatus) {
        this.label = label;
        this.targetStatus = targetStatus;
    }

    public String getLabel() {
        return label;
    }

    public AlertStatus getTargetStatus() {
        return targetStatus;
    }

    public static Optional<HandlingAction> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(a -> a.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }
}
