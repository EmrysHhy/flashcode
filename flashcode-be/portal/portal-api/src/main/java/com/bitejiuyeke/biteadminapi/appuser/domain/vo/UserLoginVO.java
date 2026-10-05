package com.bitejiuyeke.biteadminapi.appuser.domain.vo;

import com.bitejiuyeke.bitecommondomain.domain.vo.LoginUserVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 当前登录用户信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserLoginVO extends LoginUserVO {

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 手机号
     */
    private String phoneNumber;

    /**
     * 邮箱
     */
    private String email;
}
