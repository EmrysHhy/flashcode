package com.bitejiuyeke.biteportalservice.flash.domain.dto.result;

import lombok.Data;

/**
 *
 * @author Emrys
 * content: 广场应用页面DTO
 */
@Data
public class SquareAppDTO {
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * userName
     */
    private String userName;
    /**
     * 应用类型
     */
    private Integer appType;
    /**
     * 应用名称
     */
    private String appName;
    /**
     * 应用描述
     */
    private String appDesc;
    /**
     * 应用截图
     */
    private String appScreenshot;

}
