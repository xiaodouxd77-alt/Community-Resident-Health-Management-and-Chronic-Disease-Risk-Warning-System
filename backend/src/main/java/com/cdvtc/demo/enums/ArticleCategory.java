package com.cdvtc.demo.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 健康知识文章分类（受控分类，不额外设计分类表）。
 */
public enum ArticleCategory {

    CHRONIC("慢病科普"),
    DIET("饮食营养"),
    EXERCISE("运动健康"),
    LIFESTYLE("生活方式"),
    ELDERLY("老年保健"),
    OTHER("其他");

    private final String label;

    ArticleCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Optional<ArticleCategory> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    public static String labelOf(String code) {
        return fromCode(code).map(ArticleCategory::getLabel).orElse(code);
    }
}
