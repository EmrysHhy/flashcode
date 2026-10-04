package com.bitejiuyeke.biteportalservice.flash.domain.vo;

import lombok.Data;

/**
 * 应用详情
 */
@Data
public class AppDetailVO {

    private Long id;

    private Long userId;

    private String appName;

    private Integer appType;

    private String previewUrl;

    private String appDoc;
}
