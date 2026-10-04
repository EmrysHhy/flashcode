package com.bitejiuyeke.portalservice.flash.service;

import com.bitejiuyeke.portalservice.flash.domain.dto.require.AppEditParam;
import com.bitejiuyeke.portalservice.flash.domain.dto.result.AppDetailDTO;
import com.bitejiuyeke.portalservice.flash.domain.dto.result.GenerateAppDTO;
import jakarta.validation.constraints.NotNull;

/**
 *
 * @author Emrys
 * content:
 */
public interface IAppService {

    //GenerateAppDTO appGenerate(Long appId, String requirement);

    /**
     * 按应用 ID 查询详情
     */
    AppDetailDTO getAppDetail(Long appId);

    /**
     * 编辑应用
     */
    GenerateAppDTO appEdit(AppEditParam appEditDTO);

    /**
     * 获取应用源代码
     * @param appId
     * @return
     */
    String getSrc(Long appId);

    /**
     * 高级编辑功能
     * @param appId
     * @return
     */
    GenerateAppDTO appAdvancedEdit(Long appId);

    /**
     * 应用公开部署
     * @param appId
     * @return
     */
    String appDeploy(@NotNull Long appId);
}
