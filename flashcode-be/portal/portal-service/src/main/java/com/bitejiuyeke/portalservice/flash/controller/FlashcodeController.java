package com.bitejiuyeke.portalservice.flash.controller;

import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.portalservice.flash.domain.dto.require.AppEditParam;
import com.bitejiuyeke.portalservice.flash.domain.dto.result.AppDetailDTO;
import com.bitejiuyeke.portalservice.flash.domain.dto.result.GenerateAppDTO;
import com.bitejiuyeke.portalservice.flash.domain.dto.result.RequirementDTO;
import com.bitejiuyeke.portalservice.flash.domain.vo.AppDetailVO;
import com.bitejiuyeke.portalservice.flash.domain.vo.GenerateAppVO;
import com.bitejiuyeke.portalservice.flash.domain.vo.RequirementVO;
import com.bitejiuyeke.portalservice.flash.service.IAppService;
import com.bitejiuyeke.portalservice.flash.service.IRequirementService;
import com.bitejiuyeke.portalservice.flash.utils.ChatContentSupport;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


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
        log.info("生成需求文档成功");
        return R.ok(requirementDTO.convertToVO());
    }
    @GetMapping("/detail")
    public R<AppDetailVO> appDetail(@RequestParam Long appId){
        log.info("收到查询应用详情请求，appId：{}", appId);
        AppDetailDTO appDetailDTO = appService.getAppDetail(appId);
        return R.ok(appDetailDTO.convertToVO());
    }
    @PostMapping("/edit")
    public R<GenerateAppVO> appEdit(@RequestBody @Validated AppEditParam appEditDTO){
        GenerateAppDTO generateAppDTO = appService.appEdit(appEditDTO);
        return R.ok(generateAppDTO.convertToVO());
    }
    @PostMapping("/getsrc")
    public R<String> getSrc(@RequestParam Long appId){
        String srcUrl = appService.getSrc(appId);
        return R.ok(srcUrl);
    }
    @PostMapping("/advanced_edit")
    public R<GenerateAppVO> appAdvancedEdit(@RequestParam @NotNull Long appId){
        GenerateAppDTO generateAppDTO = appService.appAdvancedEdit(appId);
        return R.ok(generateAppDTO.convertToVO());
    }
    @PostMapping("/app_deploy")
    public R<String> appDeploy(@RequestParam @NotNull Long appId){
        return R.ok(appService.appDeploy(appId));

    }
}
