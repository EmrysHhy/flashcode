package com.bitejiuyeke.biteportalservice.flash.service;

import com.bitejiuyeke.biteportalservice.flash.domain.dto.result.RequirementDTO;

/**
 *
 * @author Emrys
 * content:
 */
public interface IRequirementService {
    RequirementDTO generateRequirement(String input);
}
