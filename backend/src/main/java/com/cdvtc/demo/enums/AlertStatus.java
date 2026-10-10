package com.cdvtc.demo.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 预警状态。流转顺序：PENDING -> PROCESSING -> RESOLVED。
 * 已完成的预警不能被普通操作直接改回待处理，如需重开必须通过明确的重开操作并留痕。
 */
public enum AlertStatus {

    PENDING("待处理"),
    PROCESSING("处理中"),
    RESOLVED("已完成");

    private final String label;

    AlertStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Optional<AlertStatus> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }
}
