package com.bitejiuyeke.portalservice.flash.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.bitejiuyeke.portalservice.flash.advisor.TokenUsageAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * ChatClient 配置
 */
@Configuration
public class ChatClientConfig {

    /**
     * 拉长 DashScope 的 HTTP 超时。对话和向量检索共用这个 RestClient。
     * 默认大约 10 秒，embedding 接口经常在返回响应头之前就超时。
     */
    @Bean
    public RestClientCustomizer dashScopeTimeoutCustomizer() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMinutes(10));
        return builder -> builder.requestFactory(requestFactory);
    }

    @Bean("chatClient")
    public ChatClient chatClient(ChatClient.Builder builder,
                                 RedisChatMemoryConfig redisChatMemoryConfig,
                                 TokenUsageAdvisor tokenUsageAdvisor) {
        MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(redisChatMemoryConfig)
                .build();

        return builder
                .defaultAdvisors(messageChatMemoryAdvisor, tokenUsageAdvisor, new SimpleLoggerAdvisor())
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .topP(0.7)
                                .enableThinking(true)
                                .incrementalOutput(true)
                                .build()
                )
                .build();
    }
}
