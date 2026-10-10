package com.cdvtc.demo.enums;

import java.util.Arrays;
import java.util.Optional;

public enum DiseaseType {

    HYPERTENSION("高血压"),
    DIABETES("糖尿病"),
    OTHER("其他慢病");

    private final String label;

    DiseaseType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Optional<DiseaseType> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(t -> t.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    public static String labelOf(String code) {
        return fromCode(code).map(DiseaseType::getLabel).orElse(code);
    }
}
