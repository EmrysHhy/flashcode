package com.bitejiuyeke.portalservice.flash.domain.dto.require;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 *
 * @author Emrys
 * content:
 */
@Data
public class AppEditParam {
    @NotNull
    private Long appId;
    private String elementSelector;
    private String newContent;
}
