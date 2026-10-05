package com.bitejiuyeke.biteadminapi.appuser.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 验证码登录请求
 */
@Data
public class CodeLoginReqParam implements Serializable {

    /**
     * 手机号，与邮箱二选一
     */
    private String phone;

    /**
     * 邮箱，与手机号二选一
     */
    private String email;

    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String code;
}
