package com.bitejiuyeke.biteportalservice.flash.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bitejiuyeke.bitecommoncore.domain.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用信息表对应的实体类
 *
 * @author Emrys
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app")
public class AppDO extends BaseDO {

    /**
     * 所属用户主键ID
     */
    private Long userId;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用描述
     */
    private String appDesc;

    /**
     * 应用需求文档
     */
    private String appDoc;

    /**
     * 应用类型：0=html, 1=vue3, 2=vue3_spring
     */
    private Integer appType;

    /**
     * 应用截图
     */
    private String appScreenshot;
}
