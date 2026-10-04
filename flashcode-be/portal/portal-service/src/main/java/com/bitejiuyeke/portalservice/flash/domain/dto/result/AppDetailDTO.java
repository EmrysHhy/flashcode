package com.bitejiuyeke.portalservice.flash.domain.dto.result;

import com.bitejiuyeke.bitecommoncore.utils.BeanCopyUtil;
import com.bitejiuyeke.portalservice.flash.domain.vo.AppDetailVO;
import lombok.Data;

/**
 * 应用详情
 */
@Data
public class AppDetailDTO {

    private Long id;

    private Long userId;

    private String appName;

    private Integer appType;

    private String previewUrl;

    private String appDoc;

    public AppDetailVO convertToVO() {
        AppDetailVO appDetailVO = new AppDetailVO();
        BeanCopyUtil.copyProperties(this, appDetailVO);
        return appDetailVO;
    }
}
