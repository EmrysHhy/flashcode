package com.bitejiuyeke.imagemcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ImageSearchTool {

    private static final int MAX_COUNT = 8;
    private static final String EMPTY = "[]";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
    private static final Pattern BAIDU_URL = Pattern.compile(
            "\"(?:thumbURL|middleURL)\"\\s*:\\s*\"(https?:\\\\?/\\\\?/[^\"\\\\]+)\"");
    private static final Pattern BING_URL = Pattern.compile(
            "murl&quot;:&quot;(https?://.+?)&quot;");

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public ImageSearchTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .build();
    }

    /**
     * 搜配图。
     * 规则：只查国内图源，顺序为 360、必应中国、百度、搜狗；凑满上限或某源失败就换下一个；URL 去重。
     */
    @Tool(name = "search_images", description = "Search photos by keyword from domestic image sites. Returns a JSON array of {url, alt}.")
    public String searchImages(
            @ToolParam(description = "Search keyword") String query,
            @ToolParam(description = "Number of photos, 1 to 8") Integer count) {
        if (query == null || query.isBlank()) {
            return EMPTY;
        }
        int limit = count == null ? MAX_COUNT : Math.min(MAX_COUNT, Math.max(1, count));
        String keyword = query.strip();
        ArrayNode hits = objectMapper.createArrayNode();
        fillSo(hits, keyword, limit);
        if (hits.size() < limit) {
            fillBing(hits, keyword, limit);
        }
        if (hits.size() < limit) {
            fillBaidu(hits, keyword, limit);
        }
        if (hits.size() < limit) {
            fillSogou(hits, keyword, limit);
        }
        log.info("国内搜图 query={}, count={}", keyword, hits.size());
        try {
            return objectMapper.writeValueAsString(hits);
        } catch (Exception e) {
            log.warn("序列化搜图结果失败: {}", e.getMessage());
            return EMPTY;
        }
    }

    /**
     * 必应中国图片。页面里的原图地址在 murl。
     */
    private void fillBing(ArrayNode hits, String query, int limit) {
        try {
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("cn.bing.com")
                            .path("/images/async")
                            .queryParam("q", query)
                            .queryParam("first", "1")
                            .queryParam("count", limit)
                            .queryParam("mmasync", "1")
                            .build())
                    .header(HttpHeaders.REFERER, "https://cn.bing.com/")
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                return;
            }
            Matcher matcher = BING_URL.matcher(body);
            while (matcher.find() && hits.size() < limit) {
                addHit(hits, matcher.group(1), "");
            }
        } catch (Exception e) {
            log.warn("必应搜图失败: {}", e.getMessage());
        }
    }

    /**
     * 百度图片。取 thumbURL，JSON 不规范时用正则抽出地址。
     */
    private void fillBaidu(ArrayNode hits, String query, int limit) {
        try {
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("image.baidu.com")
                            .path("/search/acjson")
                            .queryParam("tn", "resultjson_com")
                            .queryParam("ipn", "rj")
                            .queryParam("ct", "201326592")
                            .queryParam("fp", "result")
                            .queryParam("ie", "utf-8")
                            .queryParam("oe", "utf-8")
                            .queryParam("word", query)
                            .queryParam("queryWord", query)
                            .queryParam("pn", "0")
                            .queryParam("rn", limit)
                            .build())
                    .header(HttpHeaders.REFERER, "https://image.baidu.com/")
                    .retrieve()
                    .body(String.class);
            int before = hits.size();
            if (body != null && !body.isBlank()) {
                try {
                    JsonNode data = objectMapper.readTree(body).path("data");
                    if (data.isArray()) {
                        for (JsonNode item : data) {
                            if (hits.size() >= limit) {
                                break;
                            }
                            String url = item.path("thumbURL").asText("");
                            if (url.isBlank()) {
                                url = item.path("middleURL").asText("");
                            }
                            addHit(hits, url, item.path("fromPageTitle").asText(""));
                        }
                    }
                } catch (Exception e) {
                    log.debug("百度 JSON 无法直接解析，改用正则: {}", e.getMessage());
                }
                if (hits.size() == before) {
                    Matcher matcher = BAIDU_URL.matcher(body);
                    while (matcher.find() && hits.size() < limit) {
                        addHit(hits, matcher.group(1).replace("\\/", "/"), "");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("百度搜图失败: {}", e.getMessage());
        }
    }

    /**
     * 360 图片。取 list[].thumb，没有则用 img。
     */
    private void fillSo(ArrayNode hits, String query, int limit) {
        try {
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("image.so.com")
                            .path("/j")
                            .queryParam("q", query)
                            .queryParam("src", "srp")
                            .queryParam("sn", "0")
                            .queryParam("pn", limit)
                            .build())
                    .header(HttpHeaders.REFERER, "https://image.so.com/")
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                return;
            }
            JsonNode list = objectMapper.readTree(body).path("list");
            if (!list.isArray()) {
                return;
            }
            for (JsonNode item : list) {
                if (hits.size() >= limit) {
                    break;
                }
                String url = item.path("thumb").asText("");
                if (url.isBlank()) {
                    url = item.path("img").asText("");
                }
                addHit(hits, url, item.path("title").asText(""));
            }
        } catch (Exception e) {
            log.warn("360 搜图失败: {}", e.getMessage());
        }
    }

    /**
     * 搜狗图片。取 picUrl，没有则用 thumbUrl。
     */
    private void fillSogou(ArrayNode hits, String query, int limit) {
        try {
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("pic.sogou.com")
                            .path("/napi/pc/searchList")
                            .queryParam("mode", "1")
                            .queryParam("start", "0")
                            .queryParam("xml_len", limit)
                            .queryParam("query", query)
                            .build())
                    .header(HttpHeaders.REFERER, "https://pic.sogou.com/")
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                return;
            }
            JsonNode items = objectMapper.readTree(body).path("data").path("items");
            if (!items.isArray()) {
                return;
            }
            for (JsonNode item : items) {
                if (hits.size() >= limit) {
                    break;
                }
                String url = item.path("picUrl").asText("");
                if (url.isBlank()) {
                    url = item.path("thumbUrl").asText("");
                }
                addHit(hits, url, item.path("title").asText(""));
            }
        } catch (Exception e) {
            log.warn("搜狗搜图失败: {}", e.getMessage());
        }
    }

    /**
     * 追加一条可用配图。
     * 规则：url 为空或已存在则跳过。
     */
    private void addHit(ArrayNode hits, String url, String alt) {
        if (url == null || url.isBlank() || containsUrl(hits, url)) {
            return;
        }
        ObjectNode hit = objectMapper.createObjectNode();
        hit.put("url", url);
        hit.put("alt", alt == null ? "" : alt);
        hits.add(hit);
    }

    private boolean containsUrl(ArrayNode hits, String url) {
        for (JsonNode hit : hits) {
            if (url.equals(hit.path("url").asText())) {
                return true;
            }
        }
        return false;
    }
}
