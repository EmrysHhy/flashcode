package com.bitejiuyeke.biteportalservice.flash.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 各模型的上下文窗口，键必须和发给 DashScope 的模型名一致。
 */
@Data
@Component
@ConfigurationProperties(prefix = "flashcode.ai")
public class AiModelWindowProperties {

    private Map<String, Long> contextWindows = new LinkedHashMap<>();

    @PostConstruct
    void fillDefaults() {
        contextWindows.putIfAbsent("qwen3-coder-plus", 1_000_000L);
        contextWindows.putIfAbsent("qwen3-vl-plus", 262_144L);
        contextWindows.putIfAbsent("qwen3.7-text-embedding-flash", 131_072L);
    }

    public Long windowOf(String model) {
        if (model == null || model.isBlank()) {
            return null;
        }
        return contextWindows.get(model);
    }
}
