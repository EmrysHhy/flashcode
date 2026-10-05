package com.bitejiuyeke.biteadminapi.appuser.feign;

import com.bitejiuyeke.biteadminapi.appuser.domain.dto.CodeLoginReqParam;
import com.bitejiuyeke.biteadminapi.appuser.domain.dto.UserEditReqParam;
import com.bitejiuyeke.biteadminapi.appuser.domain.dto.WechatLoginReqParam;
import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommondomain.domain.vo.TokenVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * C端用户登录与修改信息
 */
@FeignClient(contextId = "appUserLoginFeignClient", value = "bite-portal", path = "/user/login")
public interface AppUserLoginFeignClient {

    /**
     * 微信登录
     * @param wechatLoginParam 微信登录信息
     * @return token
     */
    @PostMapping("/wechat")
    R<TokenVO> login(@RequestBody @Validated WechatLoginReqParam wechatLoginParam);

    /**
     * 验证码登录
     * @param codeLoginParam 验证码登录信息
     * @return token
     */
    @PostMapping("/code")
    R<TokenVO> login(@RequestBody @Validated CodeLoginReqParam codeLoginParam);

    /**
     * 发送验证码
     * @param account 手机号或邮箱
     * @param email 邮箱，与 account 二选一
     * @return 验证码
     */
    @GetMapping("/send_code")
    R<String> sendCode(@RequestParam(value = "account", required = false) String account,
                       @RequestParam(value = "email", required = false) String email);

    /**
     * 退出登录
     * @return void
     */
    @DeleteMapping("/logout")
    R<Void> logout();

    /**
     * 修改当前用户信息
     * @param userEditParam 用户编辑信息
     * @return void
     */
    @PostMapping("/edit")
    R<Void> edit(@RequestBody @Validated UserEditReqParam userEditParam);

    /**
     * 上传当前用户头像
     * @param file 头像文件
     * @return 头像 URL
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    R<String> updateAvatar(@RequestPart("file") MultipartFile file);
}
