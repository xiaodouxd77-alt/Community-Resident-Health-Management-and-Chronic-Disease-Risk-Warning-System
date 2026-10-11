package com.cdvtc.demo.enums;

import java.util.Arrays;
import java.util.Optional;

/** 干预执行情况。 */
public enum ExecutionStatus {

    COMPLETED("已执行"),
    PARTIAL("部分执行"),
    NOT_COMPLETED("未执行"),
    NEEDS_ADJUSTMENT("需要调整");

    private final String label;

    ExecutionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 是否需要医生跟进（未执行 / 需要调整）。 */
    public boolean needsAttention() {
        return this == NOT_COMPLETED || this == NEEDS_ADJUSTMENT;
    }

    public static Optional<ExecutionStatus> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    public static String labelOf(String code) {
        return fromCode(code).map(ExecutionStatus::getLabel).orElse(code);
    }
}
