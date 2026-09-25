package com.bitejiuyeke.biteportalservice.flash.domain.dto.require;

import com.bitejiuyeke.bitecommondomain.domain.dto.BasePageReqDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 *
 * @author Emrys
 * content:
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SquareListReqParam extends BasePageReqDTO implements Serializable {
    private Integer appType;
}
