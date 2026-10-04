package com.bitejiuyeke.portalservice.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 验证码登录
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CodeLoginDTO extends LoginDTO {

    /**
     * 手机号（与邮箱二选一）
     */
    private String phone;

    /**
     * 邮箱（与手机号二选一）
     */
    private String email;

    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String code;
}
