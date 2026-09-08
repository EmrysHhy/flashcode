package com.bitejiuyeke.bitecommoncore.domain.enums;

import lombok.Getter;

/**
 * 拒绝策略枚举
 */
@Getter
public enum RejectType {

    /**
     * AbortPolicy策略
     */
    AbortPolicy(1),

    /**
     * CallerRunsPolicy策略
     */
    CallerRunsPolicy(2),

    /**
     * DiscardOldestPolicy策略
     */
    DiscardOldestPolicy(3),

    /**
     * DiscardPolicy策略
     */
    DiscardPolicy(4);


    private Integer value;

    RejectType(Integer value) {
        this.value = value;
    }
}
