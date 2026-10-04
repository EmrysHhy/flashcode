package com.bitejiuyeke.biteadminservice.user.domain.entity;

import com.bitejiuyeke.bitecommoncore.domain.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户对象 sys_user
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUser extends BaseDO {

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 手机号
     */
    private String phoneNumber;

    /**
     * 密码
     */
    private String password;

    /**
     * 身份
     */
    private String identity;

    /**
     * 状态
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
