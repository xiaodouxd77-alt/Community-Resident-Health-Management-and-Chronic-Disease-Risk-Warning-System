package com.cdvtc.demo.enums;

import java.util.Arrays;
import java.util.Optional;

/** 随访方式。 */
public enum FollowupMethod {

    PHONE("电话"),
    OFFLINE("线下"),
    ONLINE("线上"),
    OTHER("其他");

    private final String label;

    FollowupMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Optional<FollowupMethod> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(m -> m.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    public static String labelOf(String code) {
        return fromCode(code).map(FollowupMethod::getLabel).orElse(code);
    }
}
