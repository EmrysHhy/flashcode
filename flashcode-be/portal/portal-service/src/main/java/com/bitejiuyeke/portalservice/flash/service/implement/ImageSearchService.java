package com.bitejiuyeke.portalservice.flash.service.implement;

import com.bitejiuyeke.portalservice.flash.domain.dto.result.StockImageDTO;
import com.bitejiuyeke.portalservice.flash.utils.ChatContentSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 生成/编辑前通过 MCP 调用 image-mcp 的 search_images。
 * 失败只记日志，不中断生成，也不在 portal 里直接搜图。
 */
@Slf4j
@Service
public class ImageSearchService {

    private static final int QUERY_MAX_LEN = 80;
    private static final int KEYWORD_COUNT = 3;
    private static final int IMAGE_COUNT = 6;
    private static final String MCP_SERVICE = "image-mcp";
    private static final int CONNECT_TIMEOUT_MS = 2000;
    private static final Pattern APP_NAME = Pattern.compile(
            "##\\s*1\\.\\s*应用名称\\s*\\r?\\n(.*?)(?=\\r?\\n\\s*##\\s*2\\.\\s*应用描述)",
            Pattern.DOTALL);
    private static final Set<String> SKIP_TITLES = Set.of(
            "应用需求文档", "需求文档", "应用名称", "应用描述", "应用核心功能");

    private final ObjectMapper objectMapper;
    private final DiscoveryClient discoveryClient;
    private final ChatClient chatClient;
    private final boolean enabled;
    private final String configuredUrl;

    public ImageSearchService(ObjectMapper objectMapper,
                              DiscoveryClient discoveryClient,
                              ChatClient chatClient,
                              @Value("${flashcode.image-search.enabled:true}") boolean enabled,
                              @Value("${flashcode.image-search.url:}") String configuredUrl) {
        this.objectMapper = objectMapper;
        this.discoveryClient = discoveryClient;
        this.chatClient = chatClient;
        this.enabled = enabled;
        this.configuredUrl = configuredUrl == null ? "" : configuredUrl.strip();
    }

    /**
     * 按需求或编辑说明搜配图。
     * 规则：先让模型写 3 条搜图词，再逐条通过 MCP 调 search_images；
     * 模型没有有效词时退回规则词，只搜一次。连不上或超时返回空列表。
     */
    public List<StockImageDTO> searchForRequirement(String requirement) {
        if (!enabled || requirement == null || requirement.isBlank()) {
            return List.of();
        }
        String baseUrl = resolveMcpUrl();
        if (baseUrl.isEmpty() || !reachable(baseUrl)) {
            log.warn("图片 MCP 不可达，跳过配图。url={}", baseUrl.isEmpty() ? "(空)" : baseUrl);
            return List.of();
        }
        List<String> queries = keywordsFromModel(requirement);
        if (queries.isEmpty()) {
            String fallback = extractQuery(requirement);
            log.info("模型未给出搜图词，改用规则词: {}", fallback);
            if (fallback.isEmpty()) {
                return List.of();
            }
            queries = List.of(fallback);
        } else {
            log.info("模型搜图词: {}", queries);
        }
        List<StockImageDTO> hits = new ArrayList<>();
        for (String query : queries) {
            if (hits.size() >= IMAGE_COUNT) {
                break;
            }
            try {
                for (StockImageDTO hit : callMcp(baseUrl, query)) {
                    if (hits.size() >= IMAGE_COUNT) {
                        break;
                    }
                    if (hits.stream().noneMatch(item -> item.url().equals(hit.url()))) {
                        hits.add(hit);
                    }
                }
            } catch (Exception e) {
                log.warn("MCP 搜图失败, query={}: {}", query, e.getMessage());
            }
        }
        if (hits.isEmpty()) {
            log.warn("搜图无结果, queries={}", queries);
        } else {
            log.info("搜图成功(MCP), queries={}, count={}", queries, hits.size());
        }
        return hits;
    }

    /**
     * 让模型根据需求写出搜图词。
     * 规则：不写入聊天记录；只收 3 行画面描述，失败返回空列表。
     */
    private List<String> keywordsFromModel(String requirement) {
        try {
            String text = ChatContentSupport.collect(chatClient.prompt()
                    .system("""
                            你只负责写图片搜索词。
                            根据用户内容输出 3 行中文搜图词，每行一个具体画面。
                            不要解释，不要序号，不要代码。""")
                    .user(requirement));
            return parseKeywords(text);
        } catch (Exception e) {
            log.warn("生成搜图词失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 解析模型输出的搜图词。
     * 规则：去掉序号和空行，跳过代码块，最多 3 条。
     */
    static List<String> parseKeywords(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> words = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String word = line.strip()
                    .replaceFirst("^[-*\\d.、]+\\s*", "")
                    .replace("`", "")
                    .strip();
            if (word.isEmpty() || word.startsWith("FILE:") || word.startsWith("```")) {
                continue;
            }
            word = clip(word);
            if (!words.contains(word)) {
                words.add(word);
            }
            if (words.size() >= KEYWORD_COUNT) {
                break;
            }
        }
        return words;
    }

    /**
     * 取出搜图关键词。
     * 规则：优先「## 1. 应用名称」正文；否则跳过「应用需求文档」这类标题，取第一行有效内容。
     */
    static String extractQuery(String requirement) {
        if (requirement == null || requirement.isBlank()) {
            return "";
        }
        Matcher named = APP_NAME.matcher(requirement);
        if (named.find()) {
            String name = named.group(1).strip().replace("\n", " ");
            if (!name.isBlank() && !"待生成".equals(name)) {
                return toKeyword(name);
            }
        }
        for (String line : requirement.split("\\R")) {
            String text = line.strip().replaceFirst("^#+\\s*", "")
                    .replaceFirst("^\\d+(\\.\\d+)*\\.?\\s*", "");
            if (text.isEmpty() || SKIP_TITLES.contains(text)) {
                continue;
            }
            return toKeyword(text);
        }
        return "";
    }

    /**
     * 把需求句收成搜图关键词。
     * 规则：去掉生成、vue、spring、应用这类说明，留下最长的中文片段。
     */
    static String toKeyword(String text) {
        String cleaned = text
                .replaceAll("(?i)vue\\s*\\+\\s*spring|vue3|vue|spring\\s*boot|spring", " ")
                .replaceAll("生成一个|生成|一个|应用|展示|不同|图片", " ")
                .replaceAll("[A-Za-z0-9+]+", " ")
                .replaceAll("[，,。；;：:、\\s]+", " ")
                .strip();
        String best = "";
        for (String part : cleaned.split(" ")) {
            if (part.length() > best.length()) {
                best = part;
            }
        }
        return best.length() >= 2 ? clip(best) : clip(text);
    }

    private static String clip(String text) {
        return text.length() > QUERY_MAX_LEN ? text.substring(0, QUERY_MAX_LEN) : text;
    }

    /**
     * 解析 image-mcp 基址。
     * 规则：配置了 url 就用配置；否则取 Nacos 上第一个 image-mcp 实例。
     */
    private String resolveMcpUrl() {
        if (!configuredUrl.isEmpty()) {
            return trimSlash(configuredUrl);
        }
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(MCP_SERVICE);
            if (instances == null || instances.isEmpty()) {
                return "";
            }
            URI uri = instances.get(0).getUri();
            return uri == null ? "" : trimSlash(uri.toString());
        } catch (Exception e) {
            log.warn("Nacos 查找 image-mcp 失败: {}", e.getMessage());
            return "";
        }
    }

    private static String trimSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * 端口不通就不要初始化 MCP，避免 SSE 握手把异常甩到线程池。
     */
    private boolean reachable(String baseUrl) {
        try {
            URI uri = URI.create(baseUrl);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }
            int port = uri.getPort();
            if (port < 0) {
                port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
            }
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * MCP SSE 调用 search_images。
     * 规则：全局 MCP 客户端保持关闭，这里按次建立连接；工具返回 [{url, alt}]。
     */
    private List<StockImageDTO> callMcp(String baseUrl, String query) {
        HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(baseUrl).build();
        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(20))
                .build();
        try {
            client.initialize();
            McpSchema.CallToolResult result = client.callTool(
                    new McpSchema.CallToolRequest("search_images", Map.of(
                            "query", query,
                            "count", IMAGE_COUNT)));
            if (result == null || Boolean.TRUE.equals(result.isError())) {
                return List.of();
            }
            return parseHits(extractText(result));
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                log.debug("关闭 MCP 客户端: {}", e.getMessage());
            }
        }
    }

    private static String extractText(McpSchema.CallToolResult result) {
        if (result.content() == null) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        for (McpSchema.Content content : result.content()) {
            if (content instanceof McpSchema.TextContent text && text.text() != null) {
                sb.append(text.text());
            }
        }
        return sb.toString();
    }

    /**
     * 解析搜图 JSON。
     * 规则：必须是 [{url, alt}, ...]，没有 url 的条目丢弃。
     */
    private List<StockImageDTO> parseHits(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) {
                return List.of();
            }
            List<StockImageDTO> hits = new ArrayList<>();
            for (JsonNode node : root) {
                String url = node.path("url").asText("");
                if (url.isBlank()) {
                    continue;
                }
                hits.add(new StockImageDTO(url, node.path("alt").asText("")));
            }
            return hits;
        } catch (Exception e) {
            log.warn("解析搜图结果失败: {}", e.getMessage());
            return List.of();
        }
    }
}
