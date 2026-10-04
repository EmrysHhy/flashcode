package com.bitejiuyeke.portalservice.user.domain.vo;

import com.bitejiuyeke.bitecommondomain.domain.vo.LoginUserVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * B端用户登录信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SysUserLoginVO extends LoginUserVO {
    /**
     * 昵称
     */
    private String nickName;

    /**
     * 身份
     */
    private String identity;

    /**
     * 状态
     */
    private String status;
}
