package com.bitejiuyeke.portalservice.flash.domain.dto.require;

import com.bitejiuyeke.bitecommondomain.domain.dto.BasePageReqDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 *
 * @author Emrys
 * content: 聊天记录请求参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChatHistoryReqParam extends BasePageReqDTO implements Serializable {
    private Long appId;
}
