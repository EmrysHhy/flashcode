package com.bitejiuyeke.biteportalservice.flash.service;

import com.bitejiuyeke.biteportalservice.flash.domain.dto.GenerateAppDTO;

/**
 *
 * @author Emrys
 * content:
 */
public interface IAppService {

    GenerateAppDTO appGenerate(Long appId, String requirement);
}
