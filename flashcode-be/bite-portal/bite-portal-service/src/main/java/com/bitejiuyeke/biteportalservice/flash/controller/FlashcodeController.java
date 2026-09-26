package com.bitejiuyeke.biteportalservice.flash.controller;

import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.biteportalservice.flash.domain.dto.result.AppDetailDTO;
import com.bitejiuyeke.biteportalservice.flash.domain.dto.result.GenerateAppDTO;
import com.bitejiuyeke.biteportalservice.flash.domain.dto.result.RequirementDTO;
import com.bitejiuyeke.biteportalservice.flash.domain.vo.AppDetailVO;
import com.bitejiuyeke.biteportalservice.flash.domain.vo.GenerateAppVO;
import com.bitejiuyeke.biteportalservice.flash.domain.vo.RequirementVO;
import com.bitejiuyeke.biteportalservice.flash.service.IAppService;
import com.bitejiuyeke.biteportalservice.flash.service.IRequirementService;
import com.bitejiuyeke.biteportalservice.flash.utils.ChatContentSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
    @Autowired
    ChatClient chatClient;
    @Autowired
    VectorStore vectorStore;

    @GetMapping("/test")
    public R<String> test(@RequestParam String input, @RequestParam String conversationId) {
        String content = ChatContentSupport.collect(chatClient.prompt()
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .advisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder().build())
                        .build())
                .user(input));
        return R.ok(content);
    }

    @PostMapping("/requirement/generate")
    public R<RequirementVO> RequestDocController(String input){
        log.info("收到生成需求文档请求，输入内容：{}", input);
        RequirementDTO requirementDTO = flashCodeService.generateRequirement(input);
        return R.ok(requirementDTO.convertToVO());
    }
   /* @PostMapping("/generate")
    public R<GenerateAppVO> appGenerate(@RequestParam Long appId, @RequestParam String requirement){
        log.info("收到生成应用请求\nappId：{}\n需求文档：{}", appId, requirement);
        GenerateAppDTO generateAppDTO = appService.appGenerate(appId,requirement);
        return R.ok(generateAppDTO.convertToVO());
    }*/
    @GetMapping("/detail")
    public R<AppDetailVO> appDetail(@RequestParam Long appId){
        log.info("收到查询应用详情请求，appId：{}", appId);
        AppDetailDTO appDetailDTO = appService.getAppDetail(appId);
        return R.ok(appDetailDTO.convertToVO());
    }
}
