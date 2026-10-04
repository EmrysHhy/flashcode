package com.bitejiuyeke.portalservice.user.service;

import com.bitejiuyeke.biteadminapi.appuser.domain.dto.AppUserDTO;
import com.bitejiuyeke.biteadminapi.appuser.domain.dto.AppUserListReqDTO;
import com.bitejiuyeke.biteadminapi.appuser.domain.dto.UserEditReqDTO;
import com.bitejiuyeke.bitecommoncore.domain.dto.BasePageDTO;

import java.util.List;

/**
 * C端用户服务层接口
 */
public interface IAppUserService {

    /**
     * 根据微信ID注册用户
     * @param openId 微信ID
     * @return C端用户DTO
     */
    AppUserDTO registerByOpenId(String openId);

    /**
     * 根据openId查询用户信息
     * @param openId 用户微信ID
     * @return C端用户DTO
     */
    AppUserDTO findByOpenId(String openId);

    /**
     * 根据手机号查询用户信息
     * @param phoneNumber 手机号
     * @return C端用户DTO
     */
    AppUserDTO findByPhone(String phoneNumber);

    /**
     * 根据手机号注册用户
     * @param phoneNumber 手机号
     * @return C端用户DTO
     */
    AppUserDTO registerByPhone(String phoneNumber);

    /**
     * 根据邮箱查询用户信息
     * @param email 邮箱
     * @return C端用户DTO
     */
    AppUserDTO findByEmail(String email);

    /**
     * 根据邮箱注册用户
     * @param email 邮箱
     * @return C端用户DTO
     */
    AppUserDTO registerByEmail(String email);

    /**
     * 编辑C端用户
     * @param userEditReqDTO C端用户DTO
     */
    void edit(UserEditReqDTO userEditReqDTO);

    /**
     * 查询C端用户
     * @param appUserListReqDTO 查询C端用户DTO
     * @return C端用户分页结果DTO
     */
    BasePageDTO<AppUserDTO> getUserList(AppUserListReqDTO appUserListReqDTO);

    /**
     * 根据用户ID获取用户信息
     * @param userId 用户ID
     * @return C端用户DTO
     */
    AppUserDTO findById(Long userId);

    /**
     * 根据用户ID列表获取用户列表信息
     * @param userIds 用户ID列表
     * @return C端用户DTO列表
     */
    List<AppUserDTO> getUserList(List<Long> userIds);
}
