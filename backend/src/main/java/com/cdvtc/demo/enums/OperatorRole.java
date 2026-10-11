package com.cdvtc.demo.enums;

import java.util.Arrays;
import java.util.Optional;

/** 干预执行记录的填写者角色，用于区分医生执行记录与居民反馈。 */
public enum OperatorRole {

    DOCTOR("医生记录"),
    RESIDENT("居民反馈");

    private final String label;

    OperatorRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Optional<OperatorRole> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(r -> r.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    public static String labelOf(String code) {
        return fromCode(code).map(OperatorRole::getLabel).orElse(code);
    }
}
