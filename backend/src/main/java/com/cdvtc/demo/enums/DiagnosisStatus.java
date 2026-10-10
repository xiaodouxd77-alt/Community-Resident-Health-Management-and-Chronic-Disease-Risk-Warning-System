package com.cdvtc.demo.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 诊断资料状态。建档默认待核实，只有医生审核后才能变为已核实，
 * 居民不能自行把资料状态改为 CONFIRMED。
 */
public enum DiagnosisStatus {

    PENDING_REVIEW("待核实"),
    CONFIRMED("资料已核实"),
    REJECTED("未通过核实");

    private final String label;

    DiagnosisStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Optional<DiagnosisStatus> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    public static String labelOf(String code) {
        return fromCode(code).map(DiagnosisStatus::getLabel).orElse(code);
    }
}
