package com.bitejiuyeke.portalservice.user.controller;

import com.bitejiuyeke.biteadminapi.appuser.domain.dto.AppUserDTO;
import com.bitejiuyeke.biteadminapi.appuser.domain.vo.AppUserVo;
import com.bitejiuyeke.biteadminapi.appuser.feign.AppUserRegisterFeignClient;
import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.portalservice.user.service.IAppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C端用户注册
 */
@RestController
@RequestMapping("/user/register")
public class UserRegisterController implements AppUserRegisterFeignClient {

    @Autowired
    private IAppUserService appUserService;

    /**
     * 通过 openId 注册用户。
     */
    @Override
    public R<AppUserVo> registerByOpenId(String openId) {
        return R.ok(appUserService.registerByOpenId(openId).convertToVO());
    }

    /**
     * 通过手机号注册用户。
     */
    @Override
    public R<AppUserVo> registerByPhone(String phoneNumber) {
        AppUserDTO appUserDTO = appUserService.registerByPhone(phoneNumber);
        if (appUserDTO == null) {
            throw new ServiceException("注册失败");
        }
        return R.ok(appUserDTO.convertToVO());
    }

    /**
     * 通过邮箱注册用户。
     */
    @Override
    public R<AppUserVo> registerByEmail(String email) {
        AppUserDTO appUserDTO = appUserService.registerByEmail(email);
        if (appUserDTO == null) {
            throw new ServiceException("注册失败");
        }
        return R.ok(appUserDTO.convertToVO());
    }
}
