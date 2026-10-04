package com.bitejiuyeke.biteadminservice.config.controller;

import com.bitejiuyeke.biteadminapi.config.domain.dto.ArgumentAddReqDTO;
import com.bitejiuyeke.biteadminapi.config.domain.dto.ArgumentEditReqDTO;
import com.bitejiuyeke.biteadminapi.config.domain.dto.ArgumentListReqDTO;
import com.bitejiuyeke.biteadminservice.BiteAdminServiceApplication;
import com.bitejiuyeke.biteadminservice.config.service.ISysArgumentService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest(classes = BiteAdminServiceApplication.class)
public class ArgumentControllerTest {

    @Resource
    private ISysArgumentService iSysArgumentService;

    @Test
    @Transactional
    void add() {
        ArgumentAddReqDTO argumentAddReqDTO = new ArgumentAddReqDTO();
        argumentAddReqDTO.setName("允许上传最多图片数");
        argumentAddReqDTO.setConfigKey("pictureMax");
        argumentAddReqDTO.setValue("9");
        Assertions.assertTrue(iSysArgumentService.add(argumentAddReqDTO) > 0L);
        Assertions.assertTrue(iSysArgumentService.add(argumentAddReqDTO) > 0L);
    }

    @Test
    @Transactional
    void list() {
        ArgumentAddReqDTO argumentAddReqDTO = new ArgumentAddReqDTO();
        argumentAddReqDTO.setName("允许上传最多图片数");
        argumentAddReqDTO.setConfigKey("pictureMax");
        argumentAddReqDTO.setValue("9");
        iSysArgumentService.add(argumentAddReqDTO);
        ArgumentListReqDTO argumentListReqDTO = new ArgumentListReqDTO();
        argumentListReqDTO.setConfigKey("pictureMax");
        Assertions.assertTrue(iSysArgumentService.list(argumentListReqDTO).getList().size() == 1);
        argumentListReqDTO.setConfigKey("picture");
        Assertions.assertTrue(iSysArgumentService.list(argumentListReqDTO).getList().size() == 0);
        argumentListReqDTO.setConfigKey("");
        argumentListReqDTO.setName("允许上");
        Assertions.assertTrue(iSysArgumentService.list(argumentListReqDTO).getList().size() == 1);
    }

    @Test
    @Transactional
    void edit() {
        ArgumentAddReqDTO argumentAddReqDTO = new ArgumentAddReqDTO();
        argumentAddReqDTO.setName("允许上传最多图片数");
        argumentAddReqDTO.setConfigKey("pictureMax");
        argumentAddReqDTO.setValue("9");
        iSysArgumentService.add(argumentAddReqDTO);
        argumentAddReqDTO.setName("允许上传文件最大多少MB");
        argumentAddReqDTO.setConfigKey("fileSizeMax");
        argumentAddReqDTO.setValue("9");
        iSysArgumentService.add(argumentAddReqDTO);
        ArgumentEditReqDTO argumentEditReqDTO = new ArgumentEditReqDTO();
        argumentEditReqDTO.setConfigKey("pictureMax");
        argumentEditReqDTO.setName("允许上传最多图片数");
        argumentEditReqDTO.setValue("90");
        Assertions.assertTrue(iSysArgumentService.edit(argumentEditReqDTO) > 0L);
        argumentEditReqDTO.setConfigKey("pictureMax");
        argumentEditReqDTO.setName("允许上传文件最大多少MB");
        argumentEditReqDTO.setValue("90");
        Assertions.assertTrue(iSysArgumentService.edit(argumentEditReqDTO) > 0L);
    }

    @Test
    @Transactional
    void getByConfigKey() {
        ArgumentAddReqDTO argumentAddReqDTO = new ArgumentAddReqDTO();
        argumentAddReqDTO.setName("允许上传最多图片数");
        argumentAddReqDTO.setConfigKey("pictureMax");
        argumentAddReqDTO.setValue("9");
        iSysArgumentService.add(argumentAddReqDTO);
        Assertions.assertTrue(iSysArgumentService.getByConfigKey("pictureMax") != null);
    }

    @Test
    @Transactional
    void getByConfigKeys() {
        ArgumentAddReqDTO argumentAddReqDTO = new ArgumentAddReqDTO();
        argumentAddReqDTO.setName("允许上传最多图片数");
        argumentAddReqDTO.setConfigKey("pictureMax");
        argumentAddReqDTO.setValue("9");
        iSysArgumentService.add(argumentAddReqDTO);
        argumentAddReqDTO.setName("允许上传文件最大多少MB");
        argumentAddReqDTO.setConfigKey("fileSizeMax");
        argumentAddReqDTO.setValue("9");
        iSysArgumentService.add(argumentAddReqDTO);
        List<String> configKeys = new ArrayList<>();
        configKeys.add("fileSizeMax");
        Assertions.assertTrue(!iSysArgumentService.getByConfigKeys(configKeys).isEmpty());
        configKeys.add("pictureMax");
        Assertions.assertTrue(iSysArgumentService.getByConfigKeys(configKeys).size() == 2);
    }
}
