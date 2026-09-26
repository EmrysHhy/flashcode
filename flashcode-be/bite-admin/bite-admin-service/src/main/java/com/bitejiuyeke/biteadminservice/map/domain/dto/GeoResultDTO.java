package com.bitejiuyeke.biteadminservice.map.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 逆地址解析的结果
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GeoResultDTO extends QQMapBaseResponseDTO {

    /**
     * 结果信息
     */
    private AddrResultDTO result;
}
