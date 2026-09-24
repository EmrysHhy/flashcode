package com.bitejiuyeke.biteportalservice.user.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * B端用户查询DTO
 */
@Data
public class SysUserListReqDTO implements Serializable {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 手机号
     */
    private String phoneNumber;

    /**
     * 状态
     */
    private String status;
}
