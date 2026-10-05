package com.bitejiuyeke.biteadminapi.appuser.domain.dto;

import com.bitejiuyeke.bitecommondomain.domain.dto.BasePageReqDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询C端用户参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AppUserListReqParam extends BasePageReqDTO  implements Serializable {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 手机号
     */
    private String phoneNumber;
    /**
     * 邮箱
     */
    private String email;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 微信openId
     */
    private String openId;
}
