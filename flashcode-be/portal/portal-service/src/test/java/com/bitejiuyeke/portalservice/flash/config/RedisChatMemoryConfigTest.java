package com.bitejiuyeke.portalservice.flash.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 *
 * @author Emrys
 * content:
 */
@SpringBootTest
@Configuration
class TestConf {
   @Bean("chatTestClient")
    ChatClient chatTestClient(ChatClient.Builder builder,RedisChatMemoryConfig redisChatMemoryConfig) {
       MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(redisChatMemoryConfig)
               .build();

        return builder
                .defaultAdvisors(memoryAdvisor,new SimpleLoggerAdvisor())
                .build();
    }

}