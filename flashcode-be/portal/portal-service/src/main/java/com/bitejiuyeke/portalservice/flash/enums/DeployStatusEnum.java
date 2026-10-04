package com.bitejiuyeke.portalservice.flash.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 应用部署状态
 */
@Getter
public enum DeployStatusEnum {

    NOT_DEPLOYED(0, "未部署"),
    DEPLOYED(1, "已部署");

    DeployStatusEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @EnumValue
    private final int value;

    private final String desc;

    public static DeployStatusEnum of(Integer value) {
        if (value == null) {
            return NOT_DEPLOYED;
        }
        for (DeployStatusEnum item : values()) {
            if (item.value == value) {
                return item;
            }
        }
        return NOT_DEPLOYED;
    }
}
