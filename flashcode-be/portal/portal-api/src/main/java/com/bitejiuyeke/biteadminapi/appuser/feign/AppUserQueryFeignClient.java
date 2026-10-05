package com.bitejiuyeke.biteadminapi.appuser.feign;

import com.bitejiuyeke.biteadminapi.appuser.domain.dto.AppUserListReqParam;
import com.bitejiuyeke.biteadminapi.appuser.domain.vo.AppUserVo;
import com.bitejiuyeke.biteadminapi.appuser.domain.vo.UserLoginVO;
import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommondomain.domain.vo.BasePageVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * C端用户查询
 */
@FeignClient(contextId = "appUserQueryFeignClient", value = "bite-portal", path = "/user/query")
public interface AppUserQueryFeignClient {

    /**
     * 根据 openId 查询用户
     * @param openId 用户微信 ID
     * @return C端用户
     */
    @GetMapping("/open_id_find")
    R<AppUserVo> findByOpenId(@RequestParam String openId);

    /**
     * 根据手机号查询用户
     * @param phoneNumber 手机号
     * @return C端用户
     */
    @GetMapping("/phone_find")
    R<AppUserVo> findByPhone(@RequestParam String phoneNumber);

    /**
     * 根据邮箱查询用户
     * @param email 邮箱
     * @return C端用户
     */
    @GetMapping("/email_find")
    R<AppUserVo> findByEmail(@RequestParam String email);

    /**
     * 根据用户 ID 查询用户
     * @param userId 用户 ID
     * @return C端用户
     */
    @GetMapping("/id_find")
    R<AppUserVo> findById(@RequestParam Long userId);

    /**
     * 根据用户 ID 列表查询用户
     * @param userIds 用户 ID 列表
     * @return C端用户列表
     */
    @PostMapping("/list")
    R<List<AppUserVo>> list(@RequestBody List<Long> userIds);

    /**
     * 分页查询用户
     * @param appUserListParam 查询条件
     * @return 分页结果
     */
    @PostMapping("/list/search")
    R<BasePageVO<AppUserVo>> list(@RequestBody AppUserListReqParam appUserListParam);

    /**
     * 获取当前登录用户信息
     * @return 登录用户信息
     */
    @GetMapping("/login_info/get")
    R<UserLoginVO> getLoginUser();
}
