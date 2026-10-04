package com.bitejiuyeke.biteadminservice.map.domain.dto;

import lombok.Data;

/**
 * 区域信息DTO
 */
@Data
public class SysRegionDTO {

    /**
     * 区域ID
     */
    private Long id;

    /**
     * 区域名称
     */
    private String name;

    /**
     * 区域全称
     */
    private String fullName;

    /**
     * 父级区域ID
     */
    private Long parentId;

    /**
     * 拼音
     */
    private String pinyin;

    /**
     * 级别
     */
    private Integer level;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 纬度
     */
    private Double latitude;

}
