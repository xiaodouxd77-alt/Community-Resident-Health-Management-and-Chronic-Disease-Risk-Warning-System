package com.cdvtc.demo.enums;

import java.util.Arrays;
import java.util.Optional;

public enum Gender {

    MALE("男"),
    FEMALE("女"),
    UNKNOWN("未知");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Optional<Gender> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(g -> g.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    public static String labelOf(String code) {
        return fromCode(code).map(Gender::getLabel).orElse(UNKNOWN.getLabel());
    }
}
