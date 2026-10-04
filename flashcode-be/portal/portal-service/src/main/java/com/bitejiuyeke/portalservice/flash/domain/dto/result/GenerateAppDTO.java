package com.bitejiuyeke.portalservice.flash.domain.dto.result;

import com.bitejiuyeke.bitecommoncore.utils.BeanCopyUtil;
import com.bitejiuyeke.portalservice.flash.domain.vo.GenerateAppVO;
import com.bitejiuyeke.portalservice.flash.enums.AppTypesEnum;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 *
 * @author Emrys
 * content:
 */
@Data
public class GenerateAppDTO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long appId;

    private AppTypesEnum appType;//类型

    private String url;//预览地址
    public GenerateAppVO convertToVO() {
        GenerateAppVO generateAppVO = new GenerateAppVO();
        BeanCopyUtil.copyProperties(this, generateAppVO);
        generateAppVO.setAppType(this.appType == null ? null : this.appType.getType());
        return generateAppVO;
    }
}
