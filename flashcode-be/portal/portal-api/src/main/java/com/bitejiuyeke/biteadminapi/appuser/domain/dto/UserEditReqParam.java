package com.bitejiuyeke.biteadminapi.appuser.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * C端用户编辑参数
 */
@Data
public class UserEditReqParam implements Serializable {

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 要补绑的手机号
     */
    private String phone;

    /**
     * 要补绑的邮箱
     */
    private String email;

    /**
     * 发给新手机号或新邮箱的验证码
     */
    private String code;
}
