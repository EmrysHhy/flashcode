package com.bitejiuyeke.biteadminservice.map.domain.dto;

import lombok.Data;

/**
 * 查询结果DTO
 */
@Data
public class SearchPoiDTO {

    /**
     * 地点名称
     */
    private String title;

    /**
     * 地点地址
     */
    private String address;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 纬度
     */
    private Double latitude;
}
