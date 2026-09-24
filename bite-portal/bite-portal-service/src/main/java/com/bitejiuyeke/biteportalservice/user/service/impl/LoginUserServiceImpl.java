package com.bitejiuyeke.biteportalservice.user.service.impl;

import com.bitejiuyeke.biteadminapi.appuser.domain.dto.AppUserDTO;
import com.bitejiuyeke.biteadminapi.appuser.domain.dto.UserEditReqDTO;
import com.bitejiuyeke.bitecommoncore.utils.VerifyUtil;
import com.bitejiuyeke.bitecommondomain.domain.ResultCode;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.bitecommonmessage.service.CaptchaService;
import com.bitejiuyeke.bitecommonsecurity.domain.dto.LoginUserDTO;
import com.bitejiuyeke.bitecommonsecurity.domain.dto.TokenDTO;
import com.bitejiuyeke.bitecommonsecurity.service.TokenService;
import com.bitejiuyeke.bitecommonsecurity.utils.JwtUtil;
import com.bitejiuyeke.bitecommonsecurity.utils.SecurityUtil;
import com.bitejiuyeke.biteportalservice.user.domain.dto.CodeLoginDTO;
import com.bitejiuyeke.biteportalservice.user.domain.dto.LoginDTO;
import com.bitejiuyeke.biteportalservice.user.domain.dto.UserDTO;
import com.bitejiuyeke.biteportalservice.user.domain.dto.WechatLoginDTO;
import com.bitejiuyeke.biteportalservice.user.service.IAppUserService;
import com.bitejiuyeke.biteportalservice.user.service.ILoginUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 门户用户服务实现类
 */
@Service
@Slf4j
public class LoginUserServiceImpl implements ILoginUserService {

    /**
     * C端用户服务
     */
    @Autowired
    private IAppUserService appUserService;

    /**
     * token服务类
     */
    @Autowired
    private TokenService tokenService;

    /**
     * 验证码服务
     */
    @Autowired
    private CaptchaService captchaService;

    /**
     * 登录逻辑
     * @param loginDTO 用户登录DTO
     * @return TokenDTO 令牌
     */
    @Override
    public TokenDTO login(LoginDTO loginDTO) {
        // 1 组装登录用户上下文
        LoginUserDTO loginUserDTO = new LoginUserDTO();
        // 2 按登录方式分发
        if (loginDTO instanceof WechatLoginDTO wechatLoginDTO) {
            loginByWechat(wechatLoginDTO, loginUserDTO);
        } else if (loginDTO instanceof CodeLoginDTO codeLoginDTO) {
            loginByCode(codeLoginDTO, loginUserDTO);
        } else {
            throw new ServiceException("不支持的登录方式", ResultCode.INVALID_PARA.getCode());
        }
        // 3 标记用户来源并签发令牌
        loginUserDTO.setUserFrom("app");
        return tokenService.createToken(loginUserDTO);
    }

    /**
     * 发送验证码（手机号或邮箱）
     *
     * @param account 手机号或邮箱
     * @return 验证码
     */
    @Override
    public String sendCode(String account) {
        return captchaService.sendCode(account);
    }

    /**
     * 修改用户信息
     * @param userEditReqDTO C端用户编辑DTO
     */
    @Override
    public void edit(UserEditReqDTO userEditReqDTO) {
        appUserService.edit(userEditReqDTO);
    }

    /**
     * 获取用户登录信息
     * @return 用户信息DTO
     */
    @Override
    public UserDTO getLoginUser() {
        // 1 获取当前登录的用户
        LoginUserDTO loginUserDTO = tokenService.getLoginUser();
        if (loginUserDTO == null) {
            throw new ServiceException("用户令牌有误", ResultCode.INVALID_PARA.getCode());
        }
        // 2 查询用户资料
        AppUserDTO appUserDTO = appUserService.findById(loginUserDTO.getUserId());
        if (appUserDTO == null) {
            throw new ServiceException("查询用户失败", ResultCode.INVALID_PARA.getCode());
        }
        // 3 对象拼装，返回结果
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(loginUserDTO, userDTO);
        BeanUtils.copyProperties(appUserDTO, userDTO);
        return userDTO;
    }

    /**
     * 退出登录
     */
    @Override
    public void logout() {
        // 1 解析令牌
        String token = SecurityUtil.getToken();
        if (StringUtils.isEmpty(token)) {
            return;
        }
        String userName = JwtUtil.getUserName(token);
        String userId = JwtUtil.getUserId(token);
        log.info("{}退出了系统, 用户ID{}", userName, userId);
        // 2 删除用户缓存记录
        tokenService.delLoginUser(token);
    }

    /**
     * 处理微信登录逻辑
     * @param wechatLoginDTO 微信登录DTO
     * @param loginUserDTO 用户生命周期对象
     */
    private void loginByWechat(WechatLoginDTO wechatLoginDTO, LoginUserDTO loginUserDTO) {
        // 1 根据openId查询，没有则注册
        AppUserDTO appUserDTO = appUserService.findByOpenId(wechatLoginDTO.getOpenId());
        if (appUserDTO == null) {
            appUserDTO = appUserService.registerByOpenId(wechatLoginDTO.getOpenId());
        }
        if (appUserDTO == null || appUserDTO.getUserId() == null) {
            throw new ServiceException("用户注册失败", ResultCode.INVALID_PARA.getCode());
        }
        // 2 设置登录信息
        loginUserDTO.setUserId(appUserDTO.getUserId());
        loginUserDTO.setUserName(appUserDTO.getNickName());
    }

    /**
     * 验证码登录处理逻辑
     * @param codeLoginDTO 验证码登录DTO
     * @param loginUserDTO 用户信息上下文DTO
     */
    private void loginByCode(CodeLoginDTO codeLoginDTO, LoginUserDTO loginUserDTO) {
        String phone = codeLoginDTO.getPhone();
        String email = codeLoginDTO.getEmail();
        boolean hasPhone = StringUtils.isNotBlank(phone);
        boolean hasEmail = StringUtils.isNotBlank(email);

        // 1 手机号和邮箱二选一
        if (hasPhone && hasEmail) {
            throw new ServiceException("手机号和邮箱不能同时填写", ResultCode.INVALID_PARA.getCode());
        }
        if (!hasPhone && !hasEmail) {
            throw new ServiceException("请提供手机号或邮箱", ResultCode.INVALID_PARA.getCode());
        }

        // 2 校验验证码后再查用户，没有则注册
        AppUserDTO appUserDTO;
        if (hasPhone) {
            if (!VerifyUtil.checkPhone(phone)) {
                throw new ServiceException("手机号格式错误", ResultCode.INVALID_PARA.getCode());
            }
            validatePhoneCode(phone, codeLoginDTO.getCode());
            appUserDTO = appUserService.findByPhone(phone);
            if (appUserDTO == null) {
                appUserDTO = appUserService.registerByPhone(phone);
            }
        } else {
            if (!VerifyUtil.checkEmail(email)) {
                throw new ServiceException("邮箱格式错误", ResultCode.INVALID_PARA.getCode());
            }
            validateEmailCode(email, codeLoginDTO.getCode());
            appUserDTO = appUserService.findByEmail(email);
            if (appUserDTO == null) {
                appUserDTO = appUserService.registerByEmail(email);
            }
        }

        if (appUserDTO == null || appUserDTO.getUserId() == null) {
            throw new ServiceException("用户注册失败", ResultCode.INVALID_PARA.getCode());
        }
        // 3 设置登录信息
        loginUserDTO.setUserId(appUserDTO.getUserId());
        loginUserDTO.setUserName(appUserDTO.getNickName());
    }

    /**
     * 校验手机验证码，通过后删除缓存
     * @param phone 手机号
     * @param code 验证码
     */
    private void validatePhoneCode(String phone, String code) {
        String cacheCode = captchaService.getCode(phone);
        if (cacheCode == null) {
            throw new ServiceException("验证码无效", ResultCode.INVALID_PARA.getCode());
        }
        if (!cacheCode.equals(code)) {
            throw new ServiceException("验证码错误", ResultCode.INVALID_PARA.getCode());
        }
        captchaService.deleteCode(phone);
    }

    /**
     * 校验邮箱验证码，通过后删除缓存
     * @param email 邮箱
     * @param code 验证码
     */
    private void validateEmailCode(String email, String code) {
        String cacheCode = captchaService.getEmailCode(email);
        if (cacheCode == null) {
            throw new ServiceException("验证码无效", ResultCode.INVALID_PARA.getCode());
        }
        if (!cacheCode.equals(code)) {
            throw new ServiceException("验证码错误", ResultCode.INVALID_PARA.getCode());
        }
        captchaService.deleteEmailCode(email);
    }
}
