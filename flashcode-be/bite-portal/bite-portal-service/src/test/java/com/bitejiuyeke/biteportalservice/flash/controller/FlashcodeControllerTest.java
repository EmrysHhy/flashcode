package com.bitejiuyeke.biteportalservice.flash.controller;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 *
 * @author Emrys
 * content:
 */
@RestController
@SpringBootTest
class FlashcodeControllerTest {
    @Resource(name = "chatTestClient")
    ChatClient chatTestClient;
    @Autowired
    VectorStore vectorStore;
    @RequestMapping("/test")
    String test1(String input , String conversationId) {
        // 测试逻辑
        return chatTestClient
                .prompt()
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .advisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder().build())
                        .build())
                .user(input)
                .call()
                .content();
    }

}