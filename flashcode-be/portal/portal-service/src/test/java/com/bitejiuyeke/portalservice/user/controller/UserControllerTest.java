package com.bitejiuyeke.portalservice.user.controller;
import com.bitejiuyeke.bitecommonmessage.service.AliSmsService;
import com.bitejiuyeke.bitecommonmessage.service.CaptchaService;
import com.bitejiuyeke.portalservice.PortalServiceApplication;
import com.bitejiuyeke.portalservice.user.domain.dto.CodeLoginDTO;
import com.bitejiuyeke.portalservice.user.domain.dto.WechatLoginDTO;
import com.bitejiuyeke.portalservice.user.service.ILoginUserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * C端用户服务单元测试
 */
@SpringBootTest(classes = PortalServiceApplication.class)
public class UserControllerTest {

    @Autowired
    private ILoginUserService userService;

    @Autowired
    private AliSmsService aliSmsService;

    @Autowired
    private CaptchaService captchaService;

    @Test
    void login() {
        WechatLoginDTO wechatLoginDTO = new WechatLoginDTO();
        wechatLoginDTO.setOpenId("123456789");
        Assertions.assertTrue(userService.login(wechatLoginDTO) != null);
    }

    @Test
    void sendMessage() {
        aliSmsService.sendMobileCode("15399385964", "123456");
    }

    @Test
    void captcha() {
        Assertions.assertTrue(captchaService.sendCode("15399385964") != null);
    }

    @Test
    void sendCode() {
        Assertions.assertTrue(userService.sendCode("18888888888") != null);
    }

    @Test
    void loginByCode() {
        String phone = "18888888888";
        String code = captchaService.sendCode(phone);
        CodeLoginDTO codeLoginDTO = new CodeLoginDTO();
        codeLoginDTO.setPhone(phone);
        codeLoginDTO.setCode(code);
        Assertions.assertTrue(userService.login(codeLoginDTO) != null);
    }
}
