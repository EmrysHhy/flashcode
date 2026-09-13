package com.bitejiuyeke.biteadminapi.config.domain.dto;

import com.bitejiuyeke.bitecommondomain.domain.dto.BasePageReqDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型列表DTO
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DictionaryTypeListReqDTO extends BasePageReqDTO {

    /**
     * 字典类型值
     */
    private String value;

    /**
     * 字典类型键
     */
    private String typeKey;
}
