package com.cdvtc.demo.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 在线咨询分类。
 */
public enum ConsultationCategory {

    HEALTH_INDICATOR("健康指标咨询"),
    CHRONIC_MANAGEMENT("慢病管理"),
    LIFESTYLE("生活方式指导"),
    OTHER("其他");

    private final String label;

    ConsultationCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Optional<ConsultationCategory> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    public static String labelOf(String code) {
        return fromCode(code).map(ConsultationCategory::getLabel).orElse(code);
    }
}
