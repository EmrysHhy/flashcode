package com.bitejiuyeke.portalservice.user.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * C端用户表对应的实体类。app_user 没有 create_time、update_time。
 */
@Data
public class AppUser {

    /**
     * 自增主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户昵称
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

    /**
     * 微信ID
     */
    private String openId;

    /**
     * 用户头像
     */
    private String avatar;
}
