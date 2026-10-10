package com.cdvtc.demo.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 风险等级。priority 越大风险越高，用于多条规则命中时的优先级取舍。
 */
public enum RiskLevel {

    LOW(1, "低风险"),
    MEDIUM(2, "中风险"),
    HIGH(3, "高风险"),
    CRITICAL(4, "严重风险");

    private final int priority;
    private final String label;

    RiskLevel(int priority, String label) {
        this.priority = priority;
        this.label = label;
    }

    public int getPriority() {
        return priority;
    }

    public String getLabel() {
        return label;
    }

    /** 是否达到生成预警的门槛（中风险及以上生成预警）。 */
    public boolean shouldAlert() {
        return this.priority >= MEDIUM.priority;
    }

    public static RiskLevel max(RiskLevel a, RiskLevel b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.priority >= b.priority ? a : b;
    }

    public static Optional<RiskLevel> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(l -> l.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }
}
