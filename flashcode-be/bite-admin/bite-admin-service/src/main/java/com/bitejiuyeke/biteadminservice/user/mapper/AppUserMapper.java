package com.bitejiuyeke.biteadminservice.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitejiuyeke.biteadminapi.appuser.domain.dto.AppUserListReqDTO;
import com.bitejiuyeke.biteadminservice.user.domain.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 操作app_user表的mapper
 */
@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {

    /**
     * 根据openId查询用户信息
     * @param openId 用户微信ID
     * @return C端用户
     */
    AppUser selectByOpenId(@Param("openId") String openId);

    /**
     * 根据手机号查询用户信息
     * @param phoneNumber 手机号
     * @return C端用户
     */
    AppUser selectByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    /**
     * 查询总数
     * @param appUserListReqDTO 查询C端用户DTO
     * @return 用户总数
     */
    Long selectCount(AppUserListReqDTO appUserListReqDTO);

    /**
     * 分页查询C端用户
     * @param appUserListReqDTO 查询C端用户DTO
     * @return 用户列表
     */
    List<AppUser> selectPage(AppUserListReqDTO appUserListReqDTO);
}
