package com.bitejiuyeke.biteportalservice.flash.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 *
 * @author Emrys
 * content:
 */
@Data
public class GenerateAppVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long appId;
    private String appType;//类型
}
