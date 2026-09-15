package com.bitejiuyeke.biteportalservice.flash.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * ChatClient 配置
 */
@Configuration
public class ChatClientConfig {

    /**
     * 拉长 DashScope HTTP 读超时。
     * 生成完整应用代码可能要几分钟，OkHttp 默认大约 10 秒就会 SocketTimeoutException。
     */
    @Bean
    public RestClientCustomizer dashScopeTimeoutCustomizer() {
        return builder -> builder.requestFactory(ClientHttpRequestFactories.get(
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(Duration.ofSeconds(30))
                        .withReadTimeout(Duration.ofMinutes(10))
        ));
    }

    @Bean("chatClient")
    public ChatClient chatClient(ChatClient.Builder builder, RedisChatMemoryConfig redisChatMemoryConfig) {
        MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(redisChatMemoryConfig)
                .build();

        return builder
                .defaultAdvisors(messageChatMemoryAdvisor, new SimpleLoggerAdvisor())
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .topP(0.7)
                                .enableThinking(true)
                                .build()
                )
                .build();
    }
}
