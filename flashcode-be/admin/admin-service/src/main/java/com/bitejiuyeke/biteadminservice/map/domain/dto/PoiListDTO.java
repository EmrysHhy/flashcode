package com.bitejiuyeke.biteadminservice.map.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 地图poi
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PoiListDTO extends QQMapBaseResponseDTO {

    /**
     * 本次搜索的结果数
     */
    private Integer count;

    /**
     * 查出来的poi列表
     */
    private List<PoiDTO> data;
}
