package com.bitejiuyeke.biteportalservice.flash.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 * @author Emrys
 * content:
 */
@Getter
@AllArgsConstructor
public enum Role {
    USER(0, "用户"),
    LLM(1, "大模型");

    private final Integer value;
    private final String role;


}
