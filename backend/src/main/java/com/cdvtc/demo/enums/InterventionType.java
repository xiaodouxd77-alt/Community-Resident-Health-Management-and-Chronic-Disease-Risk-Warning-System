package com.cdvtc.demo.enums;

import java.util.Arrays;
import java.util.Optional;

/** 健康干预类型。 */
public enum InterventionType {

    LIFESTYLE("生活方式指导"),
    DIET("饮食指导"),
    EXERCISE("运动指导"),
    MONITORING("健康指标监测"),
    FOLLOWUP("随访管理"),
    OTHER("其他");

    private final String label;

    InterventionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Optional<InterventionType> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(t -> t.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    public static String labelOf(String code) {
        return fromCode(code).map(InterventionType::getLabel).orElse(code);
    }
}
