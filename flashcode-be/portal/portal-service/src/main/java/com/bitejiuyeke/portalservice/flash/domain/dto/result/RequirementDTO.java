package com.bitejiuyeke.portalservice.flash.domain.dto.result;

import com.bitejiuyeke.portalservice.flash.domain.vo.RequirementVO;
import lombok.Data;
import org.springframework.beans.BeanUtils;


/**
 *
 * @author Emrys
 * content:
 */
@Data
public class RequirementDTO {
    private Long appId;
    private String requirement;

    public RequirementVO convertToVO() {
        RequirementVO requirementVO = new RequirementVO();
        BeanUtils.copyProperties(this, requirementVO);
        return requirementVO;
    }
}
