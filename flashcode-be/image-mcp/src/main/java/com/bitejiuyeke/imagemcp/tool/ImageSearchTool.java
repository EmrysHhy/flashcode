package com.bitejiuyeke.imagemcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Slf4j
@Component
public class ImageSearchTool {

    private static final int MAX_COUNT = 6;
    private static final String EMPTY = "[]";
    private static final String USER_AGENT = "FlashcodeImageMcp/1.0 (image search; flashcode)";

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String pexelsApiKey;

    public ImageSearchTool(ObjectMapper objectMapper,
                           @Value("${image-search.pexels-api-key:}") String pexelsApiKey) {
        this.objectMapper = objectMapper;
        this.pexelsApiKey = pexelsApiKey == null ? "" : pexelsApiKey;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .build();
    }

    /**
     * 搜免版权配图。
     * 规则：有 Pexels 密钥先搜 Pexels（最多 6 张，url 用 src.large）；
     * 密钥为空或失败则搜中文维基百科配图；失败返回 []，不抛给调用方。
     */
    @Tool(name = "search_images", description = "Search royalty-free photos by keyword. Returns a JSON array of {url, alt}.")
    public String searchImages(
            @ToolParam(description = "Search keyword") String query,
            @ToolParam(description = "Number of photos, 1 to 6") Integer count) {
        if (query == null || query.isBlank()) {
            return EMPTY;
        }
        int perPage = count == null ? MAX_COUNT : Math.min(MAX_COUNT, Math.max(1, count));
        String keyword = query.strip();
        if (!pexelsApiKey.isBlank()) {
            String pexels = searchPexels(keyword, perPage);
            if (!EMPTY.equals(pexels)) {
                return pexels;
            }
        }
        return searchZhWiki(keyword, perPage);
    }

    /**
     * 调 Pexels Search。
     * 规则：Authorization 头直接放 API Key（不加 Bearer）；失败返回 []。
     */
    private String searchPexels(String query, int perPage) {
        try {
            String body = restClient.get()
                    .uri("https://api.pexels.com/v1/search?query={q}&per_page={n}", query, perPage)
                    .header("Authorization", pexelsApiKey)
                    .retrieve()
                    .body(String.class);
            return toPexelsHits(body);
        } catch (Exception e) {
            log.warn("Pexels 搜图失败，改用中文维基: {}", e.getMessage());
            return EMPTY;
        }
    }

    /**
     * 调中文维基百科搜条目缩略图。
     * 规则：比 commons.wikimedia.org 更容易从国内访问；取 pageimages thumbnail。
     */
    private String searchZhWiki(String query, int perPage) {
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
                            .queryParam("gsrlimit", perPage)
                            .queryParam("prop", "pageimages")
                            .queryParam("piprop", "thumbnail")
                            .queryParam("pithumbsize", "1280")
                            .queryParam("origin", "*")
                            .build())
                    .retrieve()
                    .body(String.class);
            return toWikiHits(body);
        } catch (Exception e) {
            log.warn("中文维基搜图失败: {}", e.getMessage());
            return EMPTY;
        }
    }

    /**
     * 转成统一 JSON：[{url, alt}]。
     * 规则：url 取 src.large，最多 MAX_COUNT 条。
     */
    private String toPexelsHits(String body) {
        if (body == null || body.isBlank()) {
            return EMPTY;
        }
        try {
            JsonNode photos = objectMapper.readTree(body).path("photos");
            ArrayNode hits = objectMapper.createArrayNode();
            if (photos.isArray()) {
                for (JsonNode photo : photos) {
                    addHit(hits, photo.path("src").path("large").asText(""), photo.path("alt").asText(""));
                    if (hits.size() >= MAX_COUNT) {
                        break;
                    }
                }
            }
            return objectMapper.writeValueAsString(hits);
        } catch (Exception e) {
            log.warn("解析 Pexels 响应失败: {}", e.getMessage());
            return EMPTY;
        }
    }

    /**
     * 转成统一 JSON：[{url, alt}]。
     * 规则：url 取 thumbnail.source，alt 用条目标题。
     */
    private String toWikiHits(String body) {
        if (body == null || body.isBlank()) {
            return EMPTY;
        }
        try {
            JsonNode pages = objectMapper.readTree(body).path("query").path("pages");
            ArrayNode hits = objectMapper.createArrayNode();
            if (pages.isObject()) {
                for (JsonNode page : pages) {
                    addHit(hits, page.path("thumbnail").path("source").asText(""), page.path("title").asText(""));
                    if (hits.size() >= MAX_COUNT) {
                        break;
                    }
                }
            }
            return objectMapper.writeValueAsString(hits);
        } catch (Exception e) {
            log.warn("解析维基搜图响应失败: {}", e.getMessage());
            return EMPTY;
        }
    }

    /**
     * 追加一条可用配图。
     * 规则：url 为空则跳过。
     */
    private void addHit(ArrayNode hits, String url, String alt) {
        if (url == null || url.isBlank()) {
            return;
        }
        ObjectNode hit = objectMapper.createObjectNode();
        hit.put("url", url);
        hit.put("alt", alt == null ? "" : alt);
        hits.add(hit);
    }
}
