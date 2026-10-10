package com.cdvtc.demo.enums;

import java.util.Arrays;
import java.util.Optional;

/** 慢病档案变更动作（追加到 chronic_disease_history）。 */
public enum ChronicAction {

    CREATE("建档"),
    UPDATE("信息更新"),
    REVIEW("资料审核"),
    STATUS_CHANGE("管理状态变更");

    private final String label;

    ChronicAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Optional<ChronicAction> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(a -> a.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    public static String labelOf(String code) {
        return fromCode(code).map(ChronicAction::getLabel).orElse(code);
    }
}
