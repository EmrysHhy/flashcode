package com.bitejiuyeke.portalservice.flash.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bitejiuyeke.bitecommoncore.domain.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 聊天历史记录表对应的实体类
 *
 * @author Emrys
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_history")
public class ChatHistoryDO extends BaseDO {

    /**
     * 所属应用主键ID
     */
    private Long appId;

    /**
     * 用户角色：0=用户, 1=大模型
     */
    private Integer chatRole;

    /**
     * 聊天内容
     */
    private String content;
}
