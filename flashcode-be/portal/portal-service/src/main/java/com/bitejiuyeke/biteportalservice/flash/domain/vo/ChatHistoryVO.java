package com.bitejiuyeke.biteportalservice.flash.domain.vo;
import lombok.Data;
/**
 *
 * @author Emrys
 * content: 聊天记录视图对象
 */
@Data
public class ChatHistoryVO {
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
