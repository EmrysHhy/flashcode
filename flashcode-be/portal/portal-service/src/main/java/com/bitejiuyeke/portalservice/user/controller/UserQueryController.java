package com.bitejiuyeke.portalservice.user.controller;

import com.bitejiuyeke.biteadminapi.appuser.domain.dto.AppUserDTO;
import com.bitejiuyeke.biteadminapi.appuser.domain.dto.AppUserListReqParam;
import com.bitejiuyeke.biteadminapi.appuser.domain.vo.AppUserVo;
import com.bitejiuyeke.biteadminapi.appuser.domain.vo.UserLoginVO;
import com.bitejiuyeke.biteadminapi.appuser.feign.AppUserQueryFeignClient;
import com.bitejiuyeke.bitecommoncore.domain.dto.BasePageDTO;
import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommondomain.domain.vo.BasePageVO;
import com.bitejiuyeke.portalservice.user.domain.vo.UserVo;
import com.bitejiuyeke.portalservice.user.service.IAppUserService;
import com.bitejiuyeke.portalservice.user.service.ILoginUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * C端用户查询
 */
@RestController
@RequestMapping("/user/query")
public class UserQueryController implements AppUserQueryFeignClient {

    @Autowired
    private IAppUserService appUserService;

    @Autowired
    private ILoginUserService loginUserService;

    /**
     * 根据 openId 查询用户。
     */
    @Override
    public R<AppUserVo> findByOpenId(String openId) {
        AppUserDTO appUserDTO = appUserService.findByOpenId(openId);
        if (appUserDTO == null) {
            return R.ok();
        }
        return R.ok(appUserDTO.convertToVO());
    }

    /**
     * 根据手机号查询用户。
     */
    @Override
    public R<AppUserVo> findByPhone(String phoneNumber) {
        AppUserDTO appUserDTO = appUserService.findByPhone(phoneNumber);
        if (appUserDTO == null) {
            return R.ok();
        }
        return R.ok(appUserDTO.convertToVO());
    }

    /**
     * 根据邮箱查询用户。
     */
    @Override
    public R<AppUserVo> findByEmail(String email) {
        AppUserDTO appUserDTO = appUserService.findByEmail(email);
        if (appUserDTO == null) {
            return R.ok();
        }
        return R.ok(appUserDTO.convertToVO());
    }

    /**
     * 根据用户 ID 查询用户。
     */
    @Override
    public R<AppUserVo> findById(Long userId) {
        AppUserDTO appUserDTO = appUserService.findById(userId);
        if (appUserDTO == null) {
            return R.ok();
        }
        return R.ok(appUserDTO.convertToVO());
    }

    /**
     * 批量查询用户列表。
     */
    @Override
    public R<List<AppUserVo>> list(List<Long> userIds) {
        List<AppUserDTO> appUserDTOList = appUserService.getUserList(userIds);
        return R.ok(appUserDTOList.stream()
                .filter(Objects::nonNull)
                .map(AppUserDTO::convertToVO)
                .collect(Collectors.toList())
        );
    }

    /**
     * 分页查询用户列表。
     */
    @Override
    public R<BasePageVO<AppUserVo>> list(AppUserListReqParam appUserListParam) {
        BasePageDTO<AppUserDTO> appUserDTOList = appUserService.getUserList(appUserListParam);
        BasePageVO<AppUserVo> result = new BasePageVO<>();
        BeanUtils.copyProperties(appUserDTOList, result);
        return R.ok(result);
    }

    /**
     * 获取当前登录用户信息。
     */
    @Override
    public R<UserLoginVO> getLoginUser() {
        UserVo userVo = loginUserService.getLoginUser().convertToVO();
        UserLoginVO userLoginVO = new UserLoginVO();
        BeanUtils.copyProperties(userVo, userLoginVO);
        return R.ok(userLoginVO);
    }
}
