package com.cdvtc.demo.enums;

import java.util.Arrays;
import java.util.Optional;

public enum ManagementStatus {

    ACTIVE("管理中"),
    SUSPENDED("暂停管理"),
    CLOSED("终止管理");

    private final String label;

    ManagementStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 是否为有效管理状态（用于重复建档校验）。 */
    public boolean isActive() {
        return this == ACTIVE || this == SUSPENDED;
    }

    public static Optional<ManagementStatus> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    public static String labelOf(String code) {
        return fromCode(code).map(ManagementStatus::getLabel).orElse(code);
    }
}
