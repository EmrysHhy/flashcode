package com.bitejiuyeke.portalservice.flash.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.bitejiuyeke.portalservice.flash.advisor.TokenUsageAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;

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

    /**
     * qwen3.8-max 这类多模态模型必须走 multimodal-generation。
     * 选项里的 multiModel 默认是 false，合并后会把请求打到纯文本接口，通义就返回 url error。
     */
    public static boolean useMultiModel(String model) {
        if (model == null || model.isBlank()) {
            return false;
        }
        String name = model.toLowerCase(Locale.ROOT);
        return name.contains("-vl")
                || name.contains("qwen3.8")
                || name.contains("qwen3.7-plus")
                || name.contains("qwen3.6-plus")
                || name.contains("qwen3.5-plus");
    }

    @Bean("chatClient")
    public ChatClient chatClient(ChatClient.Builder builder,
                                 RedisChatMemoryConfig redisChatMemoryConfig,
                                 TokenUsageAdvisor tokenUsageAdvisor,
                                 @Value("${spring.ai.dashscope.chat.options.model:qwen3-coder-plus}") String model) {
        MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(redisChatMemoryConfig)
                .build();

        return builder
                .defaultAdvisors(messageChatMemoryAdvisor, tokenUsageAdvisor, new SimpleLoggerAdvisor())
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .model(model)
                                .multiModel(useMultiModel(model))
                                .topP(0.7)
                                .enableThinking(true)
                                .incrementalOutput(true)
                                .build()
                )
                .build();
    }
}
