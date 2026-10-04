package com.bitejiuyeke.portalservice.flash.utils;

import org.springframework.ai.chat.client.ChatClient;

import java.util.stream.Collectors;

/**
 * qwen3 要求 incremental_output=true，只能 stream 后再拼全文，不能 .call()。
 */
public final class ChatContentSupport {

    private ChatContentSupport() {
    }

    public static String collect(ChatClient.ChatClientRequestSpec spec) {
        String content = spec.stream().content().collect(Collectors.joining()).block();
        return content == null ? "" : content;
    }
}
