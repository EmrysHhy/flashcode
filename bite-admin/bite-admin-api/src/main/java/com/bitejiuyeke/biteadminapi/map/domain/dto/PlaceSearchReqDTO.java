package com.bitejiuyeke.biteadminapi.map.domain.dto;

import com.bitejiuyeke.bitecommondomain.domain.dto.BasePageReqDTO;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 查询请求DTO
 */
@Data
public class PlaceSearchReqDTO extends BasePageReqDTO {

    /**
     * 请求的关键字
     */
    @NotNull(message = "请求关键字不允许为空")
    private String keyword;

    /**
     * 请求区域ID
     */
    @NotNull(message = "请求区域ID不能为空")
    private Long id;
}
