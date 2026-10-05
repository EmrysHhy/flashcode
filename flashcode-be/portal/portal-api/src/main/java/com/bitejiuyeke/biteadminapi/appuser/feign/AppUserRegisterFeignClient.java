package com.bitejiuyeke.biteadminapi.appuser.feign;

import com.bitejiuyeke.biteadminapi.appuser.domain.vo.AppUserVo;
import com.bitejiuyeke.bitecommondomain.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * C端用户注册
 */
@FeignClient(contextId = "appUserRegisterFeignClient", value = "bite-portal", path = "/user/register")
public interface AppUserRegisterFeignClient {

    /**
     * 根据微信注册用户
     * @param openId 用户微信 ID
     * @return C端用户
     */
    @GetMapping("/openid")
    R<AppUserVo> registerByOpenId(@RequestParam String openId);

    /**
     * 根据手机号注册用户
     * @param phoneNumber 手机号
     * @return C端用户
     */
    @GetMapping("/phone")
    R<AppUserVo> registerByPhone(@RequestParam String phoneNumber);

    /**
     * 根据邮箱注册用户
     * @param email 邮箱
     * @return C端用户
     */
    @GetMapping("/email")
    R<AppUserVo> registerByEmail(@RequestParam String email);
}
