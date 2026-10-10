package com.cdvtc.demo.enums;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 健康指标类型。
 * 每种类型定义了规范单位、可接受单位别名、是否需要第二数值（舒张压）以及测量场景是否必填。
 */
public enum IndicatorType {

    BLOOD_PRESSURE("血压", "mmHg", true, false,
            new LinkedHashSet<>(Arrays.asList("mmHg"))),

    BLOOD_GLUCOSE("血糖", "mmol/L", false, true,
            new LinkedHashSet<>(Arrays.asList("mmol/L", "mmol/l"))),

    WEIGHT("体重", "kg", false, false,
            new LinkedHashSet<>(Arrays.asList("kg", "公斤"))),

    HEART_RATE("心率", "bpm", false, false,
            new LinkedHashSet<>(Arrays.asList("bpm", "次/分")));

    private final String label;
    private final String defaultUnit;
    private final boolean requiresSecondaryValue;
    private final boolean measurementContextRequired;
    private final Set<String> acceptedUnits;

    IndicatorType(String label, String defaultUnit, boolean requiresSecondaryValue,
                  boolean measurementContextRequired, Set<String> acceptedUnits) {
        this.label = label;
        this.defaultUnit = defaultUnit;
        this.requiresSecondaryValue = requiresSecondaryValue;
        this.measurementContextRequired = measurementContextRequired;
        this.acceptedUnits = acceptedUnits;
    }

    public String getLabel() {
        return label;
    }

    public String getDefaultUnit() {
        return defaultUnit;
    }

    public boolean isRequiresSecondaryValue() {
        return requiresSecondaryValue;
    }

    public boolean isMeasurementContextRequired() {
        return measurementContextRequired;
    }

    public boolean acceptsUnit(String unit) {
        return unit != null && acceptedUnits.contains(unit.trim());
    }

    public static Optional<IndicatorType> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(t -> t.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }
}
