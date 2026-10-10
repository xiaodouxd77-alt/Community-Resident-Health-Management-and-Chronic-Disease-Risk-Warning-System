package com.cdvtc.demo.enums;

import java.util.Arrays;
import java.util.Optional;

public enum GroupStatus {

    ACTIVE("启用"),
    DISABLED("停用");

    private final String label;

    GroupStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Optional<GroupStatus> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    public static String labelOf(String code) {
        return fromCode(code).map(GroupStatus::getLabel).orElse(code);
    }
}
