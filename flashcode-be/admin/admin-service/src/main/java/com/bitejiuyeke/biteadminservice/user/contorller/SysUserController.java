package com.bitejiuyeke.biteadminservice.user.contorller;

import com.bitejiuyeke.biteadminservice.user.domain.dto.PasswordLoginDTO;
import com.bitejiuyeke.biteadminservice.user.domain.dto.SysUserDTO;
import com.bitejiuyeke.biteadminservice.user.domain.dto.SysUserListReqDTO;
import com.bitejiuyeke.biteadminservice.user.domain.vo.SysUserLoginVO;
import com.bitejiuyeke.biteadminservice.user.domain.vo.SysUserVo;
import com.bitejiuyeke.biteadminservice.user.service.ISysUserService;
import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommondomain.domain.vo.TokenVO;
import com.bitejiuyeke.bitecommonsecurity.domain.dto.TokenDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * B端用户服务控制器类
 */
@RestController
@RequestMapping("/sys_user1")
public class SysUserController {

    @Autowired
    private ISysUserService sysUserService;

    /**
     * B端用户登录
     *
     * @param passwordLoginDTO B端用户登录DTO
     * @return token信息
     */
    @PostMapping("/login/password")
    public R<TokenVO> login(@Validated @RequestBody PasswordLoginDTO passwordLoginDTO) {
        TokenDTO tokenDTO = sysUserService.login(passwordLoginDTO);
        return R.ok(tokenDTO.convertToVo());
    }

    /**
     * 新增或编辑用户
     * @param sysUserDTO B端用户信息
     * @return  用户ID
     */
    @PostMapping("/add_edit")
    public R<Long> addOrEditUser(@RequestBody SysUserDTO sysUserDTO) {
        return R.ok(sysUserService.addOrEdit(sysUserDTO));
    }

    /**
     * 查询B端用户
     * @param sysUserListReqDTO 用户查询DTO
     * @return B用户列表
     */
    @PostMapping("/list")
    public R<List<SysUserVo>> getUserList(@RequestBody SysUserListReqDTO sysUserListReqDTO) {
        List<SysUserDTO> sysUserDTOS = sysUserService.getUserList(sysUserListReqDTO);
        return R.ok(sysUserDTOS.stream()
                .map(SysUserDTO::convertToVO)
                .collect(Collectors.toList())
        );
    }

    /**
     * 获取B端登录用户信息
     * @return B端用户信息VO
     */
    @GetMapping("/login_info/get")
    public R<SysUserLoginVO> getLoginUser() {
        return R.ok(sysUserService.getLoginUser().convertToVO());
    }

}
