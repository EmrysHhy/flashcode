package com.bitejiuyeke.portalservice.flash.service;

import com.bitejiuyeke.portalservice.flash.domain.dto.result.RequirementDTO;

/**
 *
 * @author Emrys
 * content:
 */
public interface IRequirementService {
    RequirementDTO generateRequirement(String input);
}
