package com.bitejiuyeke.biteportalservice.user.controller;

import com.bitejiuyeke.biteadminapi.appuser.domain.dto.UserEditReqDTO;
import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommondomain.domain.vo.TokenVO;
import com.bitejiuyeke.biteportalservice.user.domain.dto.CodeLoginDTO;
import com.bitejiuyeke.biteportalservice.user.domain.dto.WechatLoginDTO;
import com.bitejiuyeke.biteportalservice.user.domain.vo.UserVo;
import com.bitejiuyeke.biteportalservice.user.service.ILoginUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 门户程序用户入口
 */
@RequestMapping("/user")
@RestController
@Slf4j
public class UserController {

    @Autowired
    private ILoginUserService userService;

    /**
     * 微信登录
     * @param wechatLoginDTO 微信登录DTO
     * @return token令牌
     */
    @PostMapping("/login/wechat")
    public R<TokenVO> login(@RequestBody @Validated WechatLoginDTO wechatLoginDTO) {
        return R.ok(userService.login(wechatLoginDTO).convertToVo());
    }

    /**
     * 发送验证码（手机号或邮箱）
     *
     * @param account 手机号或邮箱
     * @return 验证码
     */
    @GetMapping("/send_code")
    public R<String> sendCode(String account) {
        return R.ok(userService.sendCode(account));
    }

    /**
     * 验证码登录
     * @param codeLoginDTO 验证码登录信息
     * @return token信息VO
     */
    @PostMapping("/login/code")
    public R<TokenVO> login(@RequestBody @Validated CodeLoginDTO codeLoginDTO) {
        return R.ok(userService.login(codeLoginDTO).convertToVo());
    }

    /**
     * 修改用户信息
     * @param userEditReqDTO C端用户编辑DTO
     * @return void
     */
    @PostMapping("/edit")
    public R<Void> edit(@RequestBody @Validated UserEditReqDTO userEditReqDTO) {
        userService.edit(userEditReqDTO);
        return R.ok();
    }

    /**
     * 获取用户登录信息
     * @return 用户信息VO
     */
    @GetMapping("/login_info/get")
    public R<UserVo> getLoginUser() {
        return R.ok(userService.getLoginUser().convertToVO());
    }

    /**
     * 退出登录
     * @return void
     */
    @DeleteMapping("/logout")
    R<Void> logout() {
        userService.logout();
        return R.ok();
    }
}
