package com.bitejiuyeke.biteadminservice.user.controller;

import com.bitejiuyeke.biteadminservice.BiteAdminServiceApplication;
import com.bitejiuyeke.biteadminservice.user.domain.dto.PasswordLoginDTO;
import com.bitejiuyeke.biteadminservice.user.domain.dto.SysUserDTO;
import com.bitejiuyeke.biteadminservice.user.domain.dto.SysUserListReqDTO;
import com.bitejiuyeke.biteadminservice.user.service.ISysUserService;
import com.bitejiuyeke.bitecommonsecurity.domain.dto.TokenDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = BiteAdminServiceApplication.class)
public class SysUserControllerTest {

    @Autowired
    private ISysUserService sysUserService;

    @Test
    void login() {
        PasswordLoginDTO passwordLoginDTO = new PasswordLoginDTO();
        passwordLoginDTO.setPhone("18888888888");
        passwordLoginDTO.setPassword("f10bd64cefdb5cf15e2f2adb5f8dfbc3");
        Assertions.assertTrue(sysUserService.login(passwordLoginDTO).getAccessToken() != null);
    }

    @Test
    @Transactional
    void addOrEdit() {
        SysUserDTO sysUserDTO = new SysUserDTO();
        sysUserDTO.setPhoneNumber("18888888887");
        sysUserDTO.setPassword("123456789");
        sysUserDTO.setIdentity("platform_admin");
        sysUserDTO.setStatus("enable");
        sysUserDTO.setNickName("zhangSan");
        Long userId = sysUserService.addOrEdit(sysUserDTO);
        sysUserDTO.setUserId(userId);
        sysUserDTO.setNickName("liSi");
        sysUserDTO.setRemark("这是李四");
        Assertions.assertTrue(sysUserService.addOrEdit(sysUserDTO) > 0L);
    }

    @Test
    void getUserList() {
        SysUserListReqDTO sysUserListReqDTO = new SysUserListReqDTO();
        sysUserListReqDTO.setPhoneNumber("18888888888");
        Assertions.assertTrue(sysUserService.getUserList(sysUserListReqDTO).size() == 1);
    }
}
