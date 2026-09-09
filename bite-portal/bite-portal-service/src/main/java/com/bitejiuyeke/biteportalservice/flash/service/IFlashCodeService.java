package com.bitejiuyeke.biteportalservice.flash.service;

import com.bitejiuyeke.biteportalservice.flash.domain.dto.RequirementDTO;

/**
 *
 * @author Emrys
 * content:
 */
public interface IFlashCodeService {
    RequirementDTO generateRequirement(String input);
}
