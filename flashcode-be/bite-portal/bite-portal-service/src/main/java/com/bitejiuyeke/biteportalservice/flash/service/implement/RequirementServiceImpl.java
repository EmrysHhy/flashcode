package com.bitejiuyeke.biteportalservice.flash.service.implement;

import com.bitejiuyeke.biteportalservice.flash.domain.dto.result.RequirementDTO;
import com.bitejiuyeke.biteportalservice.flash.domain.entity.AppDO;
import com.bitejiuyeke.biteportalservice.flash.mapper.AppMapper;
import com.bitejiuyeke.biteportalservice.flash.service.IRequirementService;
import com.bitejiuyeke.biteportalservice.flash.utils.ChatContentSupport;
import com.bitejiuyeke.bitecommonsecurity.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Emrys
 * content:
 */
@Service
@Slf4j
public class RequirementServiceImpl implements IRequirementService {
    @Autowired
    ChatClient chatClient;
    @Autowired
    AppMapper appMapper;
    @Autowired
    TokenService tokenService;

    /**
     * 文档生成
     * @param input
     * @return
     */
    @Override
    public RequirementDTO generateRequirement(String input) {
        AppDO appDO = new AppDO();
        appDO.setUserId(tokenService.getLoginUser().getUserId());
        appDO.setAppName("待生成");
        appDO.setAppDesc("待生成");
        appMapper.insert(appDO);
        String conversationId = String.valueOf(appDO.getId());
        String content = ChatContentSupport.collect(chatClient.prompt()
                .system(getSysPrompt())
                .user(input)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId)));
        AppDO parsed = buildAppDO(content);
        appDO.setAppName(parsed.getAppName());
        appDO.setAppDesc(parsed.getAppDesc());
        appDO.setAppDoc(parsed.getAppDoc());
        appMapper.updateById(appDO);

        //返回DTO
        RequirementDTO requirementDTO = new RequirementDTO();
        requirementDTO.setAppId(appDO.getId());
        requirementDTO.setRequirement(appDO.getAppDoc());
        return requirementDTO;
    }



    /**
     * 需求文档模版
     * @return
     */
    private String getSysPrompt(){
        return String.join("\n",
                "你是资深产品经理。根据用户提供的需求，生成正式且简洁的应用需求文档.请使用Markdown 严格排版，采用如下结构与编号：",
                "# 应用需求文档",
                "## 1. 应用名称",
                "## 2. 应用描述",
                "## 3. 应用核心功能",
                "核心功能采用 3.1、3.2... 的编号格式分点呈现；若用户已明确输入核心功能，完全以用户输入内容为准；若用户未提及核心功能，结合用户输入内容生成不超过两个极简功能，所有功能均为列表类功能，所有功能均独立实现，不依赖任何第三方服务。需求文档中不提及任何第三方服务或平台。",
                "注意：仅输出符合以上要求的需求文档正文，不要任何的附加说明或多余内容");
    }

    /**
     * 通过正则表达式封装应用文档、应用名称和应用描述
     */
    private AppDO buildAppDO(String content) {
        AppDO appDO = new AppDO();
        appDO.setAppDoc(content);
        Pattern appNamePattern = Pattern.compile(
                "##\\s*1\\.\\s*应用名称\\s*\\r?\\n(.*?)(?=\\r?\\n\\s*##\\s*2\\.\\s*应用描述)",
                Pattern.DOTALL
        );
        Matcher appNameMatcher = appNamePattern.matcher(content);
        if (appNameMatcher.find()) {
            appDO.setAppName(appNameMatcher.group(1).trim());
        }

        Pattern appDescPattern = Pattern.compile(
                "##\\s*2\\.\\s*应用描述\\s*\\r?\\n(.*?)(?=\\r?\\n\\s*##\\s*3\\.\\s*应用核心功能)",
                Pattern.DOTALL
        );
        Matcher appDescMatcher = appDescPattern.matcher(content);
        if (appDescMatcher.find()) {
            appDO.setAppDesc(appDescMatcher.group(1).trim());
        }

        return appDO;
    }

}
