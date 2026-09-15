package com.bitejiuyeke.biteportalservice.flash.domain.dto;

import lombok.Data;

/**
 *
 * @author Emrys
 * content:
 */
@Data
public class RedisChatHistoryDTO {
    /**
     * id
     */
    private Long id;
    /**
     * 身份 0用户 1大模型
     */
    private Integer chatRole;
    /**
     * 单条聊天记录
     */
    private String content;
}
