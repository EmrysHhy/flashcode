package com.bitejiuyeke.portalservice.flash.advisor;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.bitejiuyeke.portalservice.flash.config.AiModelWindowProperties;
import com.bitejiuyeke.portalservice.flash.constants.FlashcodeConstant;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 按 userId、appId、实际模型记录 token 和最近一次上下文占用。
 * 向量检索的 embedding 不经过 ChatClient，不在这里记账。
 */
@Component
@Slf4j
public class TokenUsageAdvisor implements BaseAdvisor {

    private final ConcurrentHashMap<String, AtomicReference<Double>> contextRates = new ConcurrentHashMap<>();

    @Autowired
    private MeterRegistry meterRegistry;
    @Autowired
    private AiModelWindowProperties modelWindows;
    @Value("${spring.ai.dashscope.chat.options.model:qwen3.8-max}")
    private String defaultChatModel;

    /**
     * 调用前记下请求里的模型名，供返回后区分实际使用的模型。
     */
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        String model = modelFromOptions(chatClientRequest.prompt() == null ? null : chatClientRequest.prompt().getOptions());
        if (StringUtils.hasText(model)) {
            chatClientRequest.context().put(FlashcodeConstant.MODEL, model);
        }
        return chatClientRequest;
    }

    /**
     * 调用完成后读取本次用量，按用户、应用、模型分别累计输入、输出和合计。
     */
    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        ChatResponse chatResponse = chatClientResponse.chatResponse();
        if (chatResponse == null || chatResponse.getMetadata() == null) {
            return chatClientResponse;
        }
        Usage usage = chatResponse.getMetadata().getUsage();
        if (usage == null) {
            return chatClientResponse;
        }
        Map<String, Object> context = chatClientResponse.context();
        Long userId = asLong(context.get(FlashcodeConstant.USER_ID));
        Long appId = asLong(context.get(FlashcodeConstant.APP_ID));
        String model = firstText(
                chatResponse.getMetadata().getModel(),
                asText(context.get(FlashcodeConstant.MODEL)),
                defaultChatModel
        );
        int promptTokens = value(usage.getPromptTokens());
        int completionTokens = value(usage.getCompletionTokens());
        int totalTokens = value(usage.getTotalTokens());
        double contextPercent = contextPercent(model, promptTokens);
        log.info("Token使用量: userId={}, appId={}, model={}, prompt={}, completion={}, total={}, contextPercent={}",
                userId, appId, model, promptTokens, completionTokens, totalTokens, contextPercent);
        record(userId, appId, model, promptTokens, completionTokens, totalTokens, contextPercent);
        return chatClientResponse;
    }

    /**
     * 写入 Prometheus 可聚合的 token 计数，以及该用户、应用、模型最近一次上下文占用。
     */
    public void record(Long userId, Long appId, String model,
                       int promptTokens, int completionTokens,
                       int totalTokens, double contextUsageRate) {
        Tags tags = Tags.of(
                FlashcodeConstant.USER_ID, safeTag(userId),
                FlashcodeConstant.APP_ID, safeTag(appId),
                FlashcodeConstant.MODEL, safeModel(model)
        );
        increment("ai_token_total", "LLM total tokens by user/app/model", tags, totalTokens);
        increment("ai_token_prompt", "LLM prompt tokens by user/app/model", tags, promptTokens);
        increment("ai_token_completion", "LLM completion tokens by user/app/model", tags, completionTokens);
        rememberContextRate(tags, contextUsageRate);
    }

    /**
     * 把本次 token 数累加到对应计数器。Prometheus 抓到的是累计值，没有新调用时数值不变。
     */
    private void increment(String name, String description, Tags tags, int amount) {
        if (amount <= 0) {
            return;
        }
        Counter.builder(name)
                .description(description)
                .tags(tags)
                .register(meterRegistry)
                .increment(amount);
    }

    /**
     * 更新该标签组合最近一次的上下文占用。同一组标签只注册一次仪表。
     */
    private void rememberContextRate(Tags tags, double rate) {
        String key = tags.toString();
        AtomicReference<Double> holder = contextRates.computeIfAbsent(key, ignored -> {
            AtomicReference<Double> created = new AtomicReference<>(rate);
            Gauge.builder("ai_context_usage_rate", created, AtomicReference::get)
                    .description("Latest LLM context window usage percent by user/app/model")
                    .tags(tags)
                    .register(meterRegistry);
            return created;
        });
        holder.set(rate);
    }

    /**
     * 用本次输入 token 除以该模型的上下文窗口，得到占用百分比，最高 100。
     */
    private double contextPercent(String model, int promptTokens) {
        if (promptTokens <= 0) {
            return 0.0;
        }
        Long window = modelWindows.windowOf(model);
        if (window == null || window <= 0) {
            return 0.0;
        }
        return Math.min(100.0, promptTokens * 100.0 / window);
    }

    /**
     * 从本次请求的模型参数里取出模型名。
     */
    private static String modelFromOptions(ChatOptions options) {
        if (options == null) {
            return null;
        }
        if (options instanceof DashScopeChatOptions dashScopeOptions && StringUtils.hasText(dashScopeOptions.getModel())) {
            return dashScopeOptions.getModel();
        }
        return options.getModel();
    }

    /**
     * 把上下文里的用户或应用编号转成 Long，无法解析时返回空。
     */
    private static Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * 取出非空字符串，其他类型返回空。
     */
    private static String asText(Object value) {
        return value instanceof String text && StringUtils.hasText(text) ? text : null;
    }

    /**
     * 按响应模型、请求模型、默认模型的顺序取第一个非空值。
     */
    private static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "unknown";
    }

    /**
     * token 数为 null 时按 0 处理。
     */
    private static int value(Integer number) {
        return number == null ? 0 : number;
    }

    /**
     * 标签值不能为空，缺少用户或应用编号时记为 unknown。
     */
    private String safeTag(Long id) {
        return id == null ? "unknown" : String.valueOf(id);
    }

    /**
     * 标签值不能为空，缺少模型名时记为 unknown。
     */
    private String safeModel(String model) {
        return StringUtils.hasText(model) ? model : "unknown";
    }

    /**
     * 尽量在调用完成后再读取用量。
     */
    @Override
    public int getOrder() {
        return 1000;
    }
}
