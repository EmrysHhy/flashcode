package com.bitejiuyeke.biteportalservice.flash.domain.dto.require;

import com.bitejiuyeke.bitecommondomain.domain.dto.BasePageReqDTO;
import jakarta.validation.constraints.NotNull;
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
public class MyselfListReqParam extends BasePageReqDTO implements Serializable {
    @NotNull(message = "应用ID不能为空")
    private Long appId;
    private Integer appType;
}
