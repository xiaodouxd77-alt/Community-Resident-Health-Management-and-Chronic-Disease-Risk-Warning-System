package com.cdvtc.demo.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 内容发布状态，健康知识与社区公告共用。
 *   DRAFT     草稿（仅管理端可见）
 *   PUBLISHED 已发布
 *   OFFLINE   已下架
 */
public enum PublishStatus {

    DRAFT("草稿"),
    PUBLISHED("已发布"),
    OFFLINE("已下架");

    private final String label;

    PublishStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Optional<PublishStatus> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    public static String labelOf(String code) {
        return fromCode(code).map(PublishStatus::getLabel).orElse(code);
    }
}
