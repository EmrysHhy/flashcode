package com.bitejiuyeke.biteportalservice.flash.service.implement;

import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.biteportalservice.flash.service.IGiteeService;
import com.bitejiuyeke.biteportalservice.flash.utils.FileWriterUtil;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 使用 Gitee 官方 Contents API 推拉源码（仓库内按 appId 分子目录）。
 * 配置放在 Nacos bite-portal-dev.yaml 的 gitee.* 下。
 */
@Slf4j
@Service
public class GiteeServiceImpl implements IGiteeService {

    @Value("${gitee.api-base:https://gitee.com/api/v5}")
    private String apiBase;

    @Value("${gitee.access-token:}")
    private String accessToken;

    @Value("${gitee.owner:}")
    private String owner;

    @Value("${gitee.repo:flash-user-code}")
    private String repo;

    @Value("${gitee.branch:master}")
    private String branch;

    private RestClient restClient;

    @PostConstruct
    void initRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(15));
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Override
    public void push(Long appId, Map<String, String> files) {
        ensureConfigured();
        if (appId == null) {
            throw new ServiceException("应用ID不能为空");
        }
        if (files == null || files.isEmpty()) {
            throw new ServiceException("没有可推送到 Gitee 的源码文件");
        }
        String resolvedOwner = resolveOwner();
        int uploaded = 0;
        for (Map.Entry<String, String> entry : files.entrySet()) {
            String repoPath = toRepoPath(appId, entry.getKey());
            if (repoPath == null) {
                continue;
            }
            upsertFile(resolvedOwner, repoPath, entry.getValue() == null ? "" : entry.getValue(),
                    "flashcode: sync " + repoPath);
            uploaded++;
        }
        if (uploaded == 0) {
            throw new ServiceException("没有合法的源码路径可推送到 Gitee");
        }
        log.info("已推送到 Gitee, owner={}, repo={}, branch={}, appId={}, files={}",
                resolvedOwner, repo, branch, appId, uploaded);
    }

    @Override
    public void pull(Long appId) {
        ensureConfigured();
        if (appId == null) {
            throw new ServiceException("应用ID不能为空");
        }
        String resolvedOwner = resolveOwner();
        JsonNode dir = get(contentsUri(resolvedOwner, String.valueOf(appId), Map.of("ref", branch)));
        if (dir == null) {
            throw new ServiceException("Gitee 上没有 appId=" + appId + " 的源码");
        }
        Map<String, String> files = new LinkedHashMap<>();
        collectFiles(resolvedOwner, String.valueOf(appId), dir, files);
        if (files.isEmpty()) {
            throw new ServiceException("Gitee 上没有 appId=" + appId + " 的源码文件");
        }
        FileWriterUtil.saveCode(appId, files);
        log.info("已从 Gitee 拉取源码, owner={}, repo={}, appId={}, files={}",
                resolvedOwner, repo, appId, files.size());
    }

    private void upsertFile(String resolvedOwner, String repoPath, String content, String message) {
        String encoded = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
        JsonNode existing = get(contentsUri(resolvedOwner, repoPath, Map.of("ref", branch)));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("access_token", accessToken);
        body.put("content", encoded);
        body.put("message", message);
        body.put("branch", branch);
        if (existing != null && StringUtils.isNotBlank(text(existing, "sha"))) {
            body.put("sha", text(existing, "sha"));
            put(contentsUri(resolvedOwner, repoPath, Map.of()), body);
            return;
        }
        post(contentsUri(resolvedOwner, repoPath, Map.of()), body);
    }

    private void collectFiles(String resolvedOwner, String appPrefix, JsonNode node, Map<String, String> files) {
        if (node == null) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectFiles(resolvedOwner, appPrefix, item, files);
            }
            return;
        }
        String type = text(node, "type");
        String path = text(node, "path");
        if ("dir".equals(type) && StringUtils.isNotBlank(path)) {
            JsonNode children = get(contentsUri(resolvedOwner, path, Map.of("ref", branch)));
            collectFiles(resolvedOwner, appPrefix, children, files);
            return;
        }
        if (!"file".equals(type) || StringUtils.isBlank(path)) {
            return;
        }
        String prefix = appPrefix.endsWith("/") ? appPrefix : appPrefix + "/";
        if (!path.startsWith(prefix)) {
            return;
        }
        String relative = path.substring(prefix.length());
        if (relative.isBlank()) {
            return;
        }
        files.put(relative, readFileContent(resolvedOwner, path, node));
    }

    private String readFileContent(String resolvedOwner, String path, JsonNode node) {
        if (node.has("content") && StringUtils.isNotBlank(node.get("content").asText())) {
            return decodeContent(text(node, "encoding"), node.get("content").asText(""));
        }
        JsonNode detail = get(contentsUri(resolvedOwner, path, Map.of("ref", branch)));
        if (detail == null) {
            return "";
        }
        return decodeContent(text(detail, "encoding"), detail.has("content") ? detail.get("content").asText("") : "");
    }

    private String decodeContent(String encoding, String content) {
        if (content == null) {
            return "";
        }
        if ("base64".equalsIgnoreCase(encoding)) {
            String compact = content.replaceAll("\\s", "");
            if (compact.isEmpty()) {
                return "";
            }
            return new String(Base64.getDecoder().decode(compact), StandardCharsets.UTF_8);
        }
        return content;
    }

    private String resolveOwner() {
        if (StringUtils.isNotBlank(owner)) {
            return owner.trim();
        }
        JsonNode user = get(apiUri("/user", Map.of()));
        String login = text(user, "login");
        if (StringUtils.isBlank(login)) {
            throw new ServiceException("无法从 Gitee Token 解析用户，请配置 gitee.owner");
        }
        return login;
    }

    private void ensureConfigured() {
        if (StringUtils.isBlank(accessToken)) {
            throw new ServiceException("未配置 gitee.access-token，请写到 Nacos bite-portal-dev.yaml");
        }
        if (StringUtils.isBlank(repo) || StringUtils.isBlank(branch)) {
            throw new ServiceException("未配置 gitee.repo 或 gitee.branch");
        }
        if (restClient == null) {
            throw new ServiceException("Gitee HTTP 客户端未初始化");
        }
    }

    private String toRepoPath(Long appId, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        String normalized = relativePath.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isEmpty() || normalized.contains("..")) {
            log.warn("跳过非法 Gitee 路径: {}", relativePath);
            return null;
        }
        String lower = normalized.toLowerCase();
        if (lower.contains("node_modules/") || lower.startsWith("node_modules")
                || lower.contains("/target/") || "/target".equals(lower) || lower.startsWith("target/")
                || lower.startsWith(".git/") || "/.git".equals(lower)) {
            log.warn("跳过构建产物路径: {}", relativePath);
            return null;
        }
        return appId + "/" + normalized;
    }

    /**
     * 完整 URL，避免 RestClient 以 / 开头的 path 丢掉 /api/v5。
     */
    private URI contentsUri(String resolvedOwner, String filePath, Map<String, String> extraQuery) {
        String encodedPath = Arrays.stream(filePath.split("/"))
                .filter(StringUtils::isNotBlank)
                .map(part -> UriUtils.encodePathSegment(part, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));
        return apiUri("/repos/" + resolvedOwner + "/" + repo + "/contents/" + encodedPath, extraQuery);
    }

    private URI apiUri(String path, Map<String, String> extraQuery) {
        String base = StringUtils.removeEnd(apiBase.trim(), "/");
        String suffix = path.startsWith("/") ? path : "/" + path;
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(base + suffix)
                .queryParam("access_token", accessToken);
        if (extraQuery != null) {
            extraQuery.forEach(builder::queryParam);
        }
        return builder.build(true).toUri();
    }

    private JsonNode get(URI uri) {
        try {
            return restClient.get().uri(uri).retrieve().body(JsonNode.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return null;
            }
            throw wrap(HttpMethod.GET.name(), uri, e);
        }
    }

    private JsonNode post(URI uri, Object body) {
        try {
            return restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            throw wrap(HttpMethod.POST.name(), uri, e);
        }
    }

    private JsonNode put(URI uri, Object body) {
        try {
            return restClient.put()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            throw wrap(HttpMethod.PUT.name(), uri, e);
        }
    }

    private ServiceException wrap(String method, URI uri, RestClientResponseException e) {
        String safeUri = uri.toString().replaceAll("access_token=[^&]+", "access_token=***");
        log.error("Gitee API {} {} 失败, status={}, body={}",
                method, safeUri, e.getStatusCode().value(), truncate(e.getResponseBodyAsString()));
        return new ServiceException("Gitee API 调用失败: " + e.getStatusCode().value()
                + " " + truncate(e.getResponseBodyAsString()));
    }

    private static String text(JsonNode node, String field) {
        if (node == null || field == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 500 ? body : body.substring(0, 500);
    }
}
