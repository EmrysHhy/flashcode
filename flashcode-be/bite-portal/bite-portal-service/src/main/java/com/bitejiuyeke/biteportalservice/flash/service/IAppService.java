package com.bitejiuyeke.biteportalservice.flash.service;

import com.bitejiuyeke.biteportalservice.flash.domain.dto.result.AppDetailDTO;
import com.bitejiuyeke.biteportalservice.flash.domain.dto.result.GenerateAppDTO;

/**
 *
 * @author Emrys
 * content:
 */
public interface IAppService {

    GenerateAppDTO appGenerate(Long appId, String requirement);

    /**
     * 按应用 ID 查询详情
     */
    AppDetailDTO getAppDetail(Long appId);
}
