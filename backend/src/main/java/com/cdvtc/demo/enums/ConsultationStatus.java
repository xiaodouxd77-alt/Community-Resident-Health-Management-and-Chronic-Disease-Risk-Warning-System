package com.cdvtc.demo.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 在线咨询状态。
 *   PENDING    待受理（居民已发起，尚无医生受理）
 *   PROCESSING 处理中（已有医生受理）
 *   RESOLVED   已解决
 *   CLOSED     已关闭（居民主动取消或医生关闭）
 */
public enum ConsultationStatus {

    PENDING("待受理"),
    PROCESSING("处理中"),
    RESOLVED("已解决"),
    CLOSED("已关闭");

    private final String label;

    ConsultationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 是否为已结束状态（不可再受理、回复、取消）。 */
    public boolean isTerminal() {
        return this == RESOLVED || this == CLOSED;
    }

    /** 居民是否可以继续补充消息（待受理、处理中）。 */
    public boolean residentCanMessage() {
        return this == PENDING || this == PROCESSING;
    }

    public static Optional<ConsultationStatus> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    public static String labelOf(String code) {
        return fromCode(code).map(ConsultationStatus::getLabel).orElse(code);
    }
}
