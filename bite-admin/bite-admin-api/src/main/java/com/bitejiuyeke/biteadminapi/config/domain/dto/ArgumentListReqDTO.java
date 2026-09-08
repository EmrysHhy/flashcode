package com.bitejiuyeke.biteadminapi.config.domain.dto;

import com.bitejiuyeke.bitecommondomain.domain.dto.BasePageReqDTO;
import lombok.Data;

/**
 * 查看参数DTO
 */
@Data
public class ArgumentListReqDTO extends BasePageReqDTO {

    /**
     * 参数名称
     */
    private String name;

    /**
     * 参数业务主键
     */
    private String configKey;
}
