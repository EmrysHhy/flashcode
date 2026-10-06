package com.bitejiuyeke.portalservice.flash.service.implement;

import com.bitejiuyeke.portalservice.flash.domain.dto.result.StockImageDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 生成/编辑前搜图。优先调 image-mcp 的 HTTP 接口，不通则直接查 Wikimedia。
 * 失败只记日志，不中断生成。
 */
@Slf4j
@Service
public class ImageSearchService {

    private static final int QUERY_MAX_LEN = 80;
    private static final int IMAGE_COUNT = 6;
    private static final String MCP_SERVICE = "image-mcp";
    private static final String USER_AGENT = "FlashcodePortal/1.0 (image search; flashcode)";
    private static final Pattern APP_NAME = Pattern.compile(
            "##\\s*1\\.\\s*应用名称\\s*\\r?\\n(.*?)(?=\\r?\\n\\s*##\\s*2\\.\\s*应用描述)",
            Pattern.DOTALL);
    private static final Set<String> SKIP_TITLES = Set.of(
            "应用需求文档", "需求文档", "应用名称", "应用描述", "应用核心功能");

    private final ObjectMapper objectMapper;
    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;
    private final boolean enabled;
    private final String configuredUrl;

    public ImageSearchService(ObjectMapper objectMapper,
                              DiscoveryClient discoveryClient,
                              @Value("${flashcode.image-search.enabled:true}") boolean enabled,
                              @Value("${flashcode.image-search.url:}") String configuredUrl) {
        this.objectMapper = objectMapper;
        this.discoveryClient = discoveryClient;
        this.enabled = enabled;
        this.configuredUrl = configuredUrl == null ? "" : configuredUrl.strip();
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .build();
    }

    /**
     * 按需求或编辑说明搜配图。
     * 规则：查询词优先取「应用名称」；先 HTTP 调 image-mcp，不通再查中文维基；
     * 任何失败返回空列表，调用方继续生成/编辑。
     */
    public List<StockImageDTO> searchForRequirement(String requirement) {
        if (!enabled) {
            return List.of();
        }
        String query = extractQuery(requirement);
        if (query.isEmpty()) {
            return List.of();
        }
        try {
            List<StockImageDTO> hits = searchViaMcp(query);
            if (!hits.isEmpty()) {
                log.info("搜图成功(image-mcp), query={}, count={}", query, hits.size());
                return hits;
            }
            hits = searchZhWiki(query);
            if (!hits.isEmpty()) {
                log.info("搜图成功(中文维基), query={}, count={}", query, hits.size());
            } else {
                log.warn("搜图无结果, query={}", query);
            }
            return hits;
        } catch (Exception e) {
            log.warn("搜图失败，跳过配图: {}", e.getMessage());
            return List.of();
        }
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
                return clip(name);
            }
        }
        for (String line : requirement.split("\\R")) {
            String text = line.strip().replaceFirst("^#+\\s*", "")
                    .replaceFirst("^\\d+(\\.\\d+)*\\.?\\s*", "");
            if (text.isEmpty() || SKIP_TITLES.contains(text)) {
                continue;
            }
            return clip(text);
        }
        return "";
    }

    private static String clip(String text) {
        return text.length() > QUERY_MAX_LEN ? text.substring(0, QUERY_MAX_LEN) : text;
    }

    /**
     * 调 image-mcp 的 HTTP 搜图接口。
     * 规则：地址优先 Nacos 配置 flashcode.image-search.url，否则发现服务名 image-mcp；
     * 不走 MCP SSE，避免容器内握手失败拖垮生成。
     */
    private List<StockImageDTO> searchViaMcp(String query) {
        String baseUrl = resolveMcpUrl();
        if (baseUrl.isEmpty()) {
            return List.of();
        }
        try {
            String json = restClient.get()
                    .uri(baseUrl + "/search_images?query={q}&count={n}", query, IMAGE_COUNT)
                    .retrieve()
                    .body(String.class);
            return parseHits(json);
        } catch (Exception e) {
            log.warn("调用 image-mcp 失败 {}: {}", baseUrl, e.getMessage());
            return List.of();
        }
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
            var uri = instances.get(0).getUri();
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
     * 中文维基百科条目缩略图。
     * 规则：国内比 commons.wikimedia.org 更容易通；失败返回空列表。
     */
    private List<StockImageDTO> searchZhWiki(String query) {
        try {
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("zh.wikipedia.org")
                            .path("/w/api.php")
                            .queryParam("action", "query")
                            .queryParam("format", "json")
                            .queryParam("generator", "search")
                            .queryParam("gsrsearch", query)
                            .queryParam("gsrlimit", IMAGE_COUNT)
                            .queryParam("prop", "pageimages")
                            .queryParam("piprop", "thumbnail")
                            .queryParam("pithumbsize", "1280")
                            .queryParam("origin", "*")
                            .build())
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                return List.of();
            }
            JsonNode pages = objectMapper.readTree(body).path("query").path("pages");
            if (!pages.isObject()) {
                return List.of();
            }
            List<StockImageDTO> hits = new ArrayList<>();
            for (JsonNode page : pages) {
                String url = page.path("thumbnail").path("source").asText("");
                if (url.isBlank()) {
                    continue;
                }
                hits.add(new StockImageDTO(url, page.path("title").asText("")));
                if (hits.size() >= IMAGE_COUNT) {
                    break;
                }
            }
            return hits;
        } catch (Exception e) {
            log.warn("中文维基搜图失败: {}", e.getMessage());
            return List.of();
        }
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
