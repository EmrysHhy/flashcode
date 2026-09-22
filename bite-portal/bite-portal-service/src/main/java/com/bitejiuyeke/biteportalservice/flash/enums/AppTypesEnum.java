package com.bitejiuyeke.biteportalservice.flash.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 *
 * @author Emrys
 * content:
 */
@Getter
public enum AppTypesEnum {
    Html(0, "Html"),
    Vue3(1, "Vue3"),
    Spring_Vue3(2, "Spring_Vue3");

    AppTypesEnum(int value, String type) {
        this.value = value;
        this.type = type;
    }

    private final int value;
    private final String type;

    @JsonCreator
    public static AppTypesEnum of(String value) {
        if (value == null || value.isBlank()) {
            return Html;
        }
        String normalized = value.trim();
        if (normalized.regionMatches(true, 0, "APP_TYPE=", 0, 9)) {
            normalized = normalized.substring(9).trim();
        }
        if ("VUE3_SPRING".equalsIgnoreCase(normalized)) {
            return Spring_Vue3;
        }
        for (AppTypesEnum item : values()) {
            if (item.name().equalsIgnoreCase(normalized)
                    || item.type.equalsIgnoreCase(normalized)
                    || String.valueOf(item.value).equals(normalized)) {
                return item;
            }
        }
        return Html;
    }

    /**
     * 提示词中的 APP_TYPE 示例，与解析器使用同一套枚举名
     */
    public static String promptAppTypes() {
        return Arrays.stream(values())
                .map(item -> "APP_TYPE=" + item.name())
                .collect(Collectors.joining("、"));
    }
}
