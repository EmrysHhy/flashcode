package com.bitejiuyeke.biteportalservice.flash.domain.dto.result;

import lombok.Data;

/**
 *
 * @author Emrys
 * content:
 */
@Data
public class ChatHistoryDTO {
    /**
     * 主键
     */
    private Long id;
    /**
     * 应用ID
     */
    private Long appId;
    /**
     * 消息角色
     */
    private int msgRole;
    /**
     * 消息内容
     */
    private String content;
}
