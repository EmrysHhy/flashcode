package com.bitejiuyeke.bitecommoncore.domain;

import lombok.Data;

@Data
public class TestRegionDTO {
    /**
     * 区域id
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
     * 区域编码
     */
    private String code;

    /**
     * 区域父id
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
