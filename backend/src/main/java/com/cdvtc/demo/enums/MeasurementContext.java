package com.cdvtc.demo.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 测量场景。血糖指标的规则依赖该上下文，空腹与餐后阈值不同。
 */
public enum MeasurementContext {

    FASTING("空腹"),
    POSTPRANDIAL("餐后2小时"),
    RANDOM("随机");

    private final String label;

    MeasurementContext(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Optional<MeasurementContext> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(c -> c.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }
}
