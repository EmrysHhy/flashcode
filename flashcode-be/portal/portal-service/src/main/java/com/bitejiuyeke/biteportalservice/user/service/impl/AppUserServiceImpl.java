package com.bitejiuyeke.biteportalservice.user.service.impl;

import com.bitejiuyeke.biteadminapi.appuser.domain.dto.AppUserDTO;
import com.bitejiuyeke.biteadminapi.appuser.domain.dto.AppUserListReqDTO;
import com.bitejiuyeke.biteadminapi.appuser.domain.dto.UserEditReqDTO;

import com.bitejiuyeke.bitecommoncore.domain.dto.BasePageDTO;
import com.bitejiuyeke.bitecommoncore.utils.AESUtil;
import com.bitejiuyeke.bitecommondomain.domain.ResultCode;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.biteportalservice.user.domain.entity.AppUser;
import com.bitejiuyeke.biteportalservice.user.mapper.AppUserMapper;
import com.bitejiuyeke.biteportalservice.user.service.IAppUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * C端用户服务层实现类
 */
@Component
@Slf4j
@RefreshScope
public class AppUserServiceImpl implements IAppUserService {

    @Autowired
    private AppUserMapper appUserMapper;

    @Value("${appuser.info.defaultAvatar}")
    private String defaultAvatar;

    /**
     * 根据微信ID注册用户
     * @param openId 微信ID
     * @return C端用户DTO
     */
    @Override
    public AppUserDTO registerByOpenId(String openId) {
        if (StringUtils.isEmpty(openId)) {
            throw new ServiceException("微信ID不能为空", ResultCode.INVALID_PARA.getCode());
        }
        AppUser appUser = new AppUser();
        appUser.setOpenId(openId);
        appUser.setNickName("比特用户"+ (int) (Math.random() * 9000) + 1000);
        appUser.setAvatar(defaultAvatar);
        appUserMapper.insert(appUser);
        AppUserDTO appUserDTO = new AppUserDTO();
        BeanUtils.copyProperties(appUser, appUserDTO);
        appUserDTO.setUserId(appUser.getId());
        return appUserDTO;
    }

    /**
     * 根据openId查询用户信息
     * @param openId 用户微信ID
     * @return C端用户DTO
     */
    @Override
    public AppUserDTO findByOpenId(String openId) {
        if (StringUtils.isEmpty(openId)) {
            return null;
        }

        // 1 查询appUser实体类
        AppUser appUser = appUserMapper.selectByOpenId(openId);

        // 2 对查出来的结果进行判断
        if (appUser == null) {
            return null;
        }
        return toAppUserDTO(appUser);
    }

    /**
     * 根据手机号查询用户信息
     * @param phoneNumber 手机号
     * @return C端用户DTO
     */
    @Override
    public AppUserDTO findByPhone(String phoneNumber) {
        // 1 对入参判空
        if (StringUtils.isEmpty(phoneNumber)) {
            return null;
        }
        // 2 执行查询语句
        AppUser appUser = appUserMapper.selectByPhoneNumber(AESUtil.encryptHex(phoneNumber));
        if (appUser == null) {
            return null;
        }
        return toAppUserDTO(appUser);
    }

    /**
     * 根据手机号注册用户
     * @param phoneNumber 手机号
     * @return C端用户DTO
     */
    @Override
    public AppUserDTO registerByPhone(String phoneNumber) {
        // 1 对手机号进行判断
        if (StringUtils.isEmpty(phoneNumber)) {
            throw new ServiceException("要注册手机号是空的", ResultCode.INVALID_PARA.getCode());
        }
        // 2 生成用户对象
        AppUser appUser = new AppUser();
        appUser.setPhoneNumber(AESUtil.encryptHex(phoneNumber));
        appUser.setNickName("比特用户"+ (int) (Math.random() * 9000) + 1000);
        appUser.setAvatar(defaultAvatar);
        appUserMapper.insert(appUser);
        AppUserDTO appUserDTO = new AppUserDTO();
        BeanUtils.copyProperties(appUser, appUserDTO);
        appUserDTO.setUserId(appUser.getId());
        return appUserDTO;
    }

    @Override
    public AppUserDTO findByEmail(String email) {
        if (StringUtils.isEmpty(email)) {
            return null;
        }
        AppUser appUser = appUserMapper.selectByEmail(AESUtil.encryptHex(email));
        if (appUser == null) {
            return null;
        }
        return toAppUserDTO(appUser);
    }

    @Override
    public AppUserDTO registerByEmail(String email) {
        if (StringUtils.isEmpty(email)) {
            throw new ServiceException("要注册邮箱是空的", ResultCode.INVALID_PARA.getCode());
        }
        AppUser appUser = new AppUser();
        appUser.setEmail(AESUtil.encryptHex(email));
        appUser.setNickName("比特用户" + (int) (Math.random() * 9000) + 1000);
        appUser.setAvatar(defaultAvatar);
        appUserMapper.insert(appUser);
        AppUserDTO appUserDTO = new AppUserDTO();
        BeanUtils.copyProperties(appUser, appUserDTO);
        appUserDTO.setEmail(email);
        appUserDTO.setUserId(appUser.getId());
        return appUserDTO;
    }

    @Override
    public void edit(UserEditReqDTO userEditReqDTO) {
        // 1 根据ID查询要编辑的用户
        AppUser appUser = appUserMapper.selectById(userEditReqDTO.getUserId());
        if (appUser == null) {
            throw new ServiceException("用户不存在", ResultCode.INVALID_PARA.getCode());
        }
        // 2 查到用户，进行编辑操作
        appUser.setNickName(userEditReqDTO.getNickName());
        appUser.setAvatar(userEditReqDTO.getAvtar());
        appUserMapper.updateById(appUser);
    }

    /**
     * 查询C端用户
     * @param appUserListReqDTO 查询C端用户DTO
     * @return C端用户分页结果DTO
     */
    @Override
    public BasePageDTO<AppUserDTO> getUserList(AppUserListReqDTO appUserListReqDTO) {
        // 1 先把手机号和邮箱转变过来
        appUserListReqDTO.setPhoneNumber(AESUtil.encryptHex(appUserListReqDTO.getPhoneNumber()));
        appUserListReqDTO.setEmail(AESUtil.encryptHex(appUserListReqDTO.getEmail()));
        BasePageDTO<AppUserDTO> result = new BasePageDTO();

        // 2 查询总数
        Long totals = appUserMapper.selectCount(appUserListReqDTO);
        if (totals == 0) {
            result.setTotals(0);
            result.setTotalPages(0);
            result.setList(new ArrayList<>());
            return result;
        }

        // 3 分页查询
        List<AppUser> appUserList = appUserMapper.selectPage(appUserListReqDTO);
        result.setTotals(totals.intValue());
        result.setTotalPages(
                BasePageDTO.calculateTotalPages(totals, appUserListReqDTO.getPageSize())
        );
        // 4 超页
        if (CollectionUtils.isEmpty(appUserList)) {
            result.setList(new ArrayList<>());
            return result;
        }
        // 5 对象列表结果转换
        result.setList(
                appUserList.stream()
                        .map(this::toAppUserDTO)
                        .collect(Collectors.toList())
        );
        return result;
    }

    /**
     * 根据用户ID获取用户信息
     * @param userId 用户ID
     * @return C端用户DTO
     */
    @Override
    public AppUserDTO findById(Long userId) {
        // 1 对userId进行判空操作
        if (userId == null) {
            return null;
        }
        // 2 查询appUser对象
        AppUser appUser = appUserMapper.selectById(userId);
        if (appUser == null) {
            return null;
        }
        return toAppUserDTO(appUser);
    }

    /**
     * 根据用户ID列表获取用户列表信息
     * @param userIds 用户ID列表
     * @return C端用户DTO列表
     */
    @Override
    public List<AppUserDTO> getUserList(List<Long> userIds) {
        // 1 对入参进行判空
        if (CollectionUtils.isEmpty(userIds)) {
            return Arrays.asList();
        }
        // 2 查询appUser列表
        List<AppUser> appUserList = appUserMapper.selectBatchIds(userIds);

        // 3 对象转换
        return appUserList.stream()
                .map(this::toAppUserDTO)
                .collect(Collectors.toList());
    }

    private AppUserDTO toAppUserDTO(AppUser appUser) {
        AppUserDTO appUserDTO = new AppUserDTO();
        BeanUtils.copyProperties(appUser, appUserDTO);
        appUserDTO.setUserId(appUser.getId());
        if (StringUtils.isNotEmpty(appUser.getPhoneNumber())) {
            appUserDTO.setPhoneNumber(AESUtil.decryptHex(appUser.getPhoneNumber()));
        }
        if (StringUtils.isNotEmpty(appUser.getEmail())) {
            appUserDTO.setEmail(AESUtil.decryptHex(appUser.getEmail()));
        }
        return appUserDTO;
    }
}
