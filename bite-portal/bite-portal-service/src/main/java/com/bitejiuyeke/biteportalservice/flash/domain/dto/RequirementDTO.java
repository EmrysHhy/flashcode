package com.bitejiuyeke.biteportalservice.flash.domain.dto;

import com.bitejiuyeke.biteportalservice.flash.domain.vo.RequirementVO;
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
