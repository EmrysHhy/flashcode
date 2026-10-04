package com.bitejiuyeke.biteadminservice.user.controller;

import com.bitejiuyeke.biteadminservice.BiteAdminServiceApplication;
import com.bitejiuyeke.bitecommonsecurity.domain.dto.LoginUserDTO;
import com.bitejiuyeke.bitecommonsecurity.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = BiteAdminServiceApplication.class)
public class TokenTest {
    @Autowired
    private TokenService tokenService;

    @Test
    void tokenTest() {
        LoginUserDTO loginUserDTO = new LoginUserDTO();
        loginUserDTO.setUserId(100L);
        loginUserDTO.setUserName("zhangSan");
        loginUserDTO.setUserFrom("sys");
        tokenService.createToken(loginUserDTO);
    }
}
