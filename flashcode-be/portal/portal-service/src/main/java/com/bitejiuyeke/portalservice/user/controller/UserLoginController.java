package com.bitejiuyeke.portalservice.user.controller;

import com.bitejiuyeke.biteadminapi.appuser.domain.dto.CodeLoginReqParam;
import com.bitejiuyeke.biteadminapi.appuser.domain.dto.UserEditReqParam;
import com.bitejiuyeke.biteadminapi.appuser.domain.dto.WechatLoginReqParam;
import com.bitejiuyeke.biteadminapi.appuser.feign.AppUserLoginFeignClient;
import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommondomain.domain.ResultCode;
import com.bitejiuyeke.bitecommondomain.domain.vo.TokenVO;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.bitefileapi.file.domain.vo.FileVO;
import com.bitejiuyeke.bitefileapi.file.feign.FileFeignClient;
import com.bitejiuyeke.portalservice.user.domain.dto.CodeLoginDTO;
import com.bitejiuyeke.portalservice.user.domain.dto.WechatLoginDTO;
import com.bitejiuyeke.portalservice.user.service.IAppUserService;
import com.bitejiuyeke.portalservice.user.service.ILoginUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * C端用户登录与修改信息
 */
@RestController
@RequestMapping("/user/login")
@Slf4j
public class UserLoginController implements AppUserLoginFeignClient {

    @Autowired
    private ILoginUserService userService;

    @Autowired
    private IAppUserService appUserService;

    @Autowired
    private FileFeignClient fileFeignClient;

    /**
     * 使用微信登录。
     */
    @Override
    public R<TokenVO> login(WechatLoginReqParam wechatLoginParam) {
        WechatLoginDTO wechatLoginDTO = new WechatLoginDTO();
        wechatLoginDTO.setOpenId(wechatLoginParam.getOpenId());
        return R.ok(userService.login(wechatLoginDTO).convertToVo());
    }

    /**
     * 使用验证码登录。
     */
    @Override
    public R<TokenVO> login(CodeLoginReqParam codeLoginParam) {
        log.info("使用验证码登录，phone={}, email={}", codeLoginParam.getPhone(), codeLoginParam.getEmail());
        CodeLoginDTO codeLoginDTO = new CodeLoginDTO();
        codeLoginDTO.setPhone(codeLoginParam.getPhone());
        codeLoginDTO.setEmail(codeLoginParam.getEmail());
        codeLoginDTO.setCode(codeLoginParam.getCode());
        log.info("已登录");
        return R.ok(userService.login(codeLoginDTO).convertToVo());
    }

    /**
     * 发送验证码。
     */
    @Override
    public R<String> sendCode(String account, String email) {
        String target = StringUtils.isNotBlank(account) ? account : email;
        log.info("发送验证码，account={}", target);
        return R.ok(userService.sendCode(target));
    }

    /**
     * 用户退出登录。
     */
    @Override
    public R<Void> logout() {
        userService.logout();
        return R.ok();
    }

    /**
     * 编辑用户信息。
     */
    @Override
    public R<Void> edit(UserEditReqParam userEditParam) {
        userService.edit(userEditParam);
        return R.ok();
    }

    /**
     * 更新用户头像。
     */
    @Override
    public R<String> updateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请上传头像", ResultCode.INVALID_PARA.getCode());
        }
        Long userId = userService.getLoginUser().getUserId();
        R<FileVO> uploaded = fileFeignClient.uploadUserAvatar(file, userId);
        if (uploaded == null || uploaded.getData() == null
                || uploaded.getCode() != ResultCode.SUCCESS.getCode()
                || StringUtils.isBlank(uploaded.getData().getUrl())) {
            throw new ServiceException("上传头像失败");
        }
        String url = uploaded.getData().getUrl();
        appUserService.updateAvatar(userId, url);
        return R.ok(url);
    }
}
