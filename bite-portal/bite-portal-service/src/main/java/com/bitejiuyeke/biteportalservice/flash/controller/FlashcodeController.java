package com.bitejiuyeke.biteportalservice.flash.controller;

import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.biteportalservice.flash.domain.dto.GenerateAppDTO;
import com.bitejiuyeke.biteportalservice.flash.domain.dto.RequirementDTO;
import com.bitejiuyeke.biteportalservice.flash.domain.vo.GenerateAppVO;
import com.bitejiuyeke.biteportalservice.flash.domain.vo.RequirementVO;
import com.bitejiuyeke.biteportalservice.flash.service.IAppService;
import com.bitejiuyeke.biteportalservice.flash.service.IRequirementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Emrys
 * content:
 */
@RequestMapping("/flashcode")
@RestController
@Slf4j
public class FlashcodeController {
    @Autowired
    IRequirementService flashCodeService;
    @Autowired
    IAppService appService;
    @PostMapping("/requirement/generate")
    public R<RequirementVO> RequestDocController(String input){
        log.info("收到生成需求文档请求，输入内容：{}", input);
        RequirementDTO requirementDTO = flashCodeService.generateRequirement(input);
        return R.ok(requirementDTO.convertToVO());
    }
    @PostMapping("/app/generate")
    public R<GenerateAppVO> appGenerate(@RequestParam Long appId, @RequestParam String requirement){
        log.info("收到生成应用请求\nappId：{}\n需求文档：{}", appId, requirement);
        GenerateAppDTO generateAppDTO = appService.appGenerate(appId,requirement);
        return R.ok(generateAppDTO.convertToVO());
    }


}
