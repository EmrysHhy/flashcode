package com.bitejiuyeke.biteadminapi.appuser.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 微信登录请求
 */
@Data
public class WechatLoginReqParam implements Serializable {

    /**
     * 微信 openId
     */
    @NotBlank(message = "微信openId不能为空")
    private String openId;
}
