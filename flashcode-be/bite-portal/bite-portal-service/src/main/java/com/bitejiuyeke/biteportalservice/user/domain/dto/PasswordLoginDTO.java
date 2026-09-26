package com.bitejiuyeke.biteportalservice.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 账号密码登录信息
 */
@Data
public class PasswordLoginDTO implements Serializable {

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
