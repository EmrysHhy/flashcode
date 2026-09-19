package com.bitejiuyeke.biteportalservice.flash.agent;

import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.biteportalservice.flash.domain.dto.GenerateAppDTO;
import com.bitejiuyeke.biteportalservice.flash.domain.dto.RequirementDTO;
import com.bitejiuyeke.biteportalservice.flash.domain.vo.GenerateAppVO;
import com.bitejiuyeke.biteportalservice.flash.domain.vo.RequirementVO;
import com.bitejiuyeke.biteportalservice.flash.service.IAppService;
import com.bitejiuyeke.biteportalservice.flash.service.IRequirementService;
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
@RequestMapping("/agent/flashcode")
@RestController
@Slf4j
public class AgentController {
    @Autowired
    IRequirementService flashCodeService;
    @Autowired
    IAppService appService;
    @Autowired
    ChatClient chatClient;
    @Autowired
    VectorStore vectorStore;

    @PostMapping("/app/generate")
    public R<GenerateAppVO> appGenerate(@RequestParam Long appId, @RequestParam String requirement){
        log.info("收到生成应用请求\nappId：{}\n需求文档：{}", appId, requirement);
        GenerateAppDTO generateAppDTO = appService.appGenerate(appId,requirement);
        return R.ok(generateAppDTO.convertToVO());
    }


}
