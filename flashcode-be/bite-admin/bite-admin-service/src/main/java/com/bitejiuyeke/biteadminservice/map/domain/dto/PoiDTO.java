package com.bitejiuyeke.biteadminservice.map.domain.dto;

import lombok.Data;

/**
 * 地点poi信息
 */
@Data
public class PoiDTO {

    /**
     * poi地点的唯一标识
     */
    private String id;

    /**
     * poi地点的名称
     */
    private String title;

    /**
     * 地址
     */
    private String address;

    /**
     * POI类型，值说明：0:普通POI / 1:公交车站 / 2:地铁站 / 3:公交线路 / 4:行政区划
     */
    private String type;

    /**
     * poi地点所处坐标
     */
    private LocationDTO location;
}
