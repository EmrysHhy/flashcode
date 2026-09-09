package com.bitejiuyeke.biteportalservice.flash.enums;

import lombok.Getter;

/**
 *
 * @author Emrys
 * content:
 */
@Getter
public enum AppTypesEnum {
    Html(0, "Html"),
    Vue(1, "Vue"),
    Spring_Vue(2,"Spring & Vue");

    AppTypesEnum(int value, String type) {
        this.value = value;
        this.type = type;
    }

    private final int value;
    private final String type;
}
