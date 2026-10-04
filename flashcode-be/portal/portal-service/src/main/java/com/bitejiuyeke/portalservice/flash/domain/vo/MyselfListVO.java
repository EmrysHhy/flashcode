package com.bitejiuyeke.portalservice.flash.domain.vo;
import lombok.Data;

/**
 *
 * @author Emrys
 * content:
 */
@Data
public class MyselfListVO{
    private Long id;
    /**
     * 用户ID
     */
    private Long userId;
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
