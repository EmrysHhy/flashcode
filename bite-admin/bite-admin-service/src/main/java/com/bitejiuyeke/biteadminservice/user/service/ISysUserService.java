package com.bitejiuyeke.biteadminservice.user.service;

import com.bitejiuyeke.biteadminservice.user.domain.dto.PasswordLoginDTO;
import com.bitejiuyeke.biteadminservice.user.domain.dto.SysUserDTO;
import com.bitejiuyeke.biteadminservice.user.domain.dto.SysUserListReqDTO;
import com.bitejiuyeke.biteadminservice.user.domain.dto.SysUserLoginDTO;
import com.bitejiuyeke.bitecommonsecurity.domain.dto.TokenDTO;

import java.util.List;

/**
 * B端用户服务接口
 */
public interface ISysUserService {

    /**
     * B端用户账户密码登录
     *
     * @param passwordLoginDTO 用户登录DTO
     * @return tokenDTO token信息
     */
    TokenDTO login(PasswordLoginDTO passwordLoginDTO);

    /**
     * 新增或编辑用户
     * @param sysUserDTO B端用户信息DTO
     * @return 用户ID
     */
    Long addOrEdit(SysUserDTO sysUserDTO);

    /**
     * 查询B端用户
     * @param sysUserListReqDTO 用户查询DTO
     * @return B用户列表
     */
    List<SysUserDTO> getUserList(SysUserListReqDTO sysUserListReqDTO);

    /**
     * 获取B端登录用户信息
     * @return B端用户信息DTO
     */
    SysUserLoginDTO getLoginUser();
}
