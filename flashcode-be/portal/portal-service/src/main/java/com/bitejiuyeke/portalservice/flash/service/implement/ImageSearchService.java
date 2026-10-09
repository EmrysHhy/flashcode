package com.bitejiuyeke.portalservice.flash.service.implement;

import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommondomain.domain.ResultCode;
import com.bitejiuyeke.bitefileapi.file.domain.vo.FileVO;
import com.bitejiuyeke.bitefileapi.file.feign.FileFeignClient;
import com.bitejiuyeke.portalservice.flash.domain.dto.result.StockImageBatch;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private static final int IMAGE_COUNT = 8;
    /** 每个搜图词最多收 3 张，三条词加起来不超过 8 张。 */
    private static final int IMAGES_PER_QUERY = 3;
    private static final int MAX_IMAGE_BYTES = 1024 * 1024;
    private static final Duration DOWNLOAD_CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration DOWNLOAD_READ_TIMEOUT = Duration.ofSeconds(4);
    /** 只写 Mozilla/5.0 时，花瓣会回 567，摄图网会回 403。 */
    private static final String DOWNLOAD_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36";
    private static final String MCP_SERVICE = "image-mcp";
    private static final int CONNECT_TIMEOUT_MS = 2000;
    private static final Pattern URL_IN_TEXT = Pattern.compile("https?://[^\\s\"'<>]+");
    private static final Pattern APP_NAME = Pattern.compile(
            "##\\s*1\\.\\s*应用名称\\s*\\r?\\n(.*?)(?=\\r?\\n\\s*##\\s*2\\.\\s*应用描述)",
            Pattern.DOTALL);
    private static final Set<String> SKIP_TITLES = Set.of(
            "应用需求文档", "需求文档", "应用名称", "应用描述", "应用核心功能");

    private final ObjectMapper objectMapper;
    private final DiscoveryClient discoveryClient;
    private final ChatClient chatClient;
    private final FileFeignClient fileFeignClient;
    private final boolean enabled;
    private final String configuredUrl;
    private final HttpClient imageClient = HttpClient.newBuilder()
            .connectTimeout(DOWNLOAD_CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ExecutorService transferPool = Executors.newVirtualThreadPerTaskExecutor();

    public ImageSearchService(ObjectMapper objectMapper,
                              DiscoveryClient discoveryClient,
                              ChatClient chatClient,
                              FileFeignClient fileFeignClient,
                              @Value("${flashcode.image-search.enabled:true}") boolean enabled,
                              @Value("${flashcode.image-search.url:}") String configuredUrl) {
        this.objectMapper = objectMapper;
        this.discoveryClient = discoveryClient;
        this.chatClient = chatClient;
        this.fileFeignClient = fileFeignClient;
        this.enabled = enabled;
        this.configuredUrl = configuredUrl == null ? "" : configuredUrl.strip();
    }

    /**
     * 按需求或编辑说明搜配图，并立刻开始把图片转存到 OSS。
     * 规则：先让模型写 3 条搜图词，再逐条通过 MCP 调 search_images；
     * 模型没有有效词时退回规则词，只搜一次。下载和上传与后续写代码并行，
     * 单张不超过 1MB。连不上、超时或转存失败时，对应 IMG_n 不换回外链。
     */
    public StockImageBatch searchForRequirement(Long appId, String requirement) {
        if (!enabled || appId == null || requirement == null || requirement.isBlank()) {
            return StockImageBatch.empty();
        }
        String baseUrl = resolveMcpUrl();
        if (baseUrl.isEmpty() || !reachable(baseUrl)) {
            log.warn("图片 MCP 不可达，跳过配图。url={}", baseUrl.isEmpty() ? "(空)" : baseUrl);
            return StockImageBatch.empty();
        }
        List<String> queries = keywordsFromModel(requirement);
        if (queries.isEmpty()) {
            String fallback = extractQuery(requirement);
            log.info("模型未给出搜图词，改用规则词: {}", fallback);
            if (fallback.isEmpty()) {
                return StockImageBatch.empty();
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
                int taken = 0;
                for (StockImageDTO hit : callMcp(baseUrl, query)) {
                    if (hits.size() >= IMAGE_COUNT || taken >= IMAGES_PER_QUERY) {
                        break;
                    }
                    if (!hit.url().startsWith("https://")) {
                        continue;
                    }
                    if (hits.stream().noneMatch(item -> item.url().equals(hit.url()))) {
                        hits.add(new StockImageDTO(hit.url(), query));
                        taken++;
                    }
                }
            } catch (Exception e) {
                log.warn("MCP 搜图失败, query={}: {}", query, e.getMessage());
            }
        }
        if (hits.isEmpty()) {
            log.warn("搜图无结果, queries={}", queries);
            return StockImageBatch.empty();
        }
        log.info("搜图成功(MCP), queries={}, count={}", queries, hits.size());
        return beginTransfer(appId, hits);
    }

    /**
     * 搜图结束后立即并行下载并上传 OSS，调用方可以同时让模型写代码。
     * 规则：先不带 Referer 下载；403/567 时再带图床自己的来源重试一次。
     * Content-Type 必须是图片；超过 1MB 或 4 秒读不完就放弃。
     * 提示词里的 url 留空，OSS 地址只在 restore 时写回。
     */
    private StockImageBatch beginTransfer(Long appId, List<StockImageDTO> hits) {
        List<StockImageDTO> hints = new ArrayList<>();
        List<CompletableFuture<String>> tasks = new ArrayList<>();
        for (StockImageDTO hit : hits) {
            hints.add(new StockImageDTO("", hit.alt()));
            String remoteUrl = hit.url();
            tasks.add(CompletableFuture.supplyAsync(() -> transferOne(appId, remoteUrl), transferPool));
        }
        log.info("配图开始转存 OSS, appId={}, count={}", appId, hints.size());
        return new StockImageBatch(hints, tasks);
    }

    private String transferOne(Long appId, String remoteUrl) {
        try {
            Downloaded image = downloadImage(remoteUrl);
            if (image == null) {
                return null;
            }
            R<FileVO> uploaded = fileFeignClient.uploadStockImage(
                    new ByteArrayMultipartFile("file", "stock." + image.ext(), image.contentType(), image.bytes()),
                    appId);
            if (uploaded == null || uploaded.getCode() != ResultCode.SUCCESS.getCode()
                    || uploaded.getData() == null || uploaded.getData().getUrl() == null
                    || uploaded.getData().getUrl().isBlank()) {
                log.warn("配图上传 OSS 失败, appId={}", appId);
                return null;
            }
            return uploaded.getData().getUrl();
        } catch (Exception e) {
            log.warn("配图转存失败, appId={}: {}", appId, e.getMessage());
            return null;
        }
    }

    private Downloaded downloadImage(String url) {
        Fetch fetched = fetchImage(url, null);
        if (fetched.image() != null) {
            return fetched.image();
        }
        if (fetched.status() == 401 || fetched.status() == 403 || fetched.status() == 567) {
            String referer = siteReferer(url);
            if (referer != null) {
                fetched = fetchImage(url, referer);
                if (fetched.image() != null) {
                    return fetched.image();
                }
            }
        }
        if (fetched.error() != null) {
            log.warn("配图下载失败, url={}: {}", url, fetched.error());
        } else if (fetched.status() > 0) {
            log.warn("配图下载失败, status={}, url={}", fetched.status(), url);
        }
        return null;
    }

    /**
     * 图床拒绝空来源时，用来源站自己的地址再请求一次。不用预览页地址，避免 360 防盗链。
     */
    private static String siteReferer(String url) {
        String host;
        try {
            host = URI.create(url).getHost();
        } catch (Exception e) {
            return null;
        }
        if (host == null || host.isBlank()) {
            return null;
        }
        String name = host.toLowerCase(Locale.ROOT);
        if (name.endsWith("huaban.com")) {
            return "https://huaban.com/";
        }
        if (name.endsWith("699pic.com")) {
            return "https://699pic.com/";
        }
        if (name.contains("baidu.com")) {
            return "https://image.baidu.com/";
        }
        if (name.contains("sogou.com")) {
            return "https://pic.sogou.com/";
        }
        if (name.contains("bing.")) {
            return "https://cn.bing.com/";
        }
        if (name.contains("qhimg") || name.endsWith("so.com")) {
            return "https://image.so.com/";
        }
        return "https://" + name + "/";
    }

    private Fetch fetchImage(String url, String referer) {
        HttpResponse<InputStream> response = null;
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(DOWNLOAD_READ_TIMEOUT)
                    .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                    .header("User-Agent", DOWNLOAD_USER_AGENT)
                    .GET();
            if (referer != null && !referer.isBlank()) {
                builder.header("Referer", referer);
            }
            response = imageClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            if (status != 200) {
                response.body().close();
                return new Fetch(null, status, null);
            }
            long announced = response.headers().firstValueAsLong("content-length").orElse(-1);
            if (announced > MAX_IMAGE_BYTES) {
                response.body().close();
                log.warn("配图超过 1MB, 放弃, url={}", url);
                return new Fetch(null, status, null);
            }
            String contentType = response.headers().firstValue("content-type").orElse("");
            String ext = imageExtension(contentType);
            if (ext == null) {
                response.body().close();
                log.warn("配图类型不是图片, type={}, url={}", contentType, url);
                return new Fetch(null, status, null);
            }
            try (InputStream in = response.body()) {
                byte[] bytes = in.readNBytes(MAX_IMAGE_BYTES + 1);
                if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
                    log.warn("配图为空或超过 1MB, url={}", url);
                    return new Fetch(null, status, null);
                }
                String type = contentType.split(";", 2)[0].strip();
                return new Fetch(new Downloaded(bytes, type, ext), status, null);
            }
        } catch (Exception e) {
            if (response != null) {
                try {
                    response.body().close();
                } catch (Exception ignored) {
                    // 失败路径只保留原始异常
                }
            }
            return new Fetch(null, 0, e.getMessage());
        }
    }

    private record Fetch(Downloaded image, int status, String error) {
    }

    private static String imageExtension(String contentType) {
        if (contentType == null) {
            return null;
        }
        String type = contentType.toLowerCase(Locale.ROOT).split(";", 2)[0].strip();
        return switch (type) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            case "image/bmp" -> "bmp";
            default -> null;
        };
    }

    private record Downloaded(byte[] bytes, String contentType, String ext) {
    }

    /**
     * 让模型根据需求写出搜图词。
     * 规则：只写页面要展示的实物、品牌、地点或人物；不写界面、卡片、评分这类页面结构；
     * 不写入聊天记录；最多 3 条，失败返回空列表。
     */
    private List<String> keywordsFromModel(String requirement) {
        try {
            String text = ChatContentSupport.collect(chatClient.prompt()
                    .system("""
                            你只负责写图片搜索词。
                            从用户内容里找出页面真正要展示的实物、品牌、地点或人物，每个对象一行。
                            每行 4 到 12 个字，必须能搜到该对象的照片。例如：公牛墙壁插座、小米智能插座。
                            不要写界面、系统、网页、截图、卡片、列表、评分、排行、推荐、应用、后台。
                            不要解释，不要序号，不要代码，不要输出网址。最多 3 行。""")
                    .user(hideUrls(requirement)));
            return parseKeywords(text);
        } catch (Exception e) {
            log.warn("生成搜图词失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 解析模型输出的搜图词。
     * 规则：去掉序号和空行，跳过代码块和界面类词，最多 3 条。
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
            if (word.isEmpty() || word.startsWith("FILE:") || word.startsWith("```") || isUiKeyword(word)) {
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
     * 界面、评分、卡片这类词搜到的是软件截图，不能当产品配图。
     */
    private static boolean isUiKeyword(String word) {
        return word.contains("界面") || word.contains("截图") || word.contains("网页")
                || word.contains("卡片") || word.contains("评分") || word.contains("后台")
                || word.contains("排行") || word.contains("推荐");
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
     * 提示词里只放 IMG_n，冒号前是这张图的主体。
     * 规则：不写网址；主体对不上的条目不能用这张图。
     */
    public static String imageHint(List<StockImageDTO> images) {
        if (images == null || images.isEmpty()) {
            return "";
        }
        StringBuilder hint = new StringBuilder("""

                【可用图片】
                每行冒号前是这张照片的主体，冒号后是唯一可用的 src。不要写网址。
                只把图片用在主体一致的那一项上。主体对不上就不要用，改用色块。
                禁止把软件界面、网页截图套到产品或品牌上。
                """);
        for (int i = 0; i < images.size(); i++) {
            StockImageDTO hit = images.get(i);
            String subject = hit.alt() == null || hit.alt().isBlank() ? "实物照片" : hit.alt();
            hint.append("- ").append(subject).append(": IMG_").append(i + 1).append('\n');
        }
        return hint.toString();
    }

    /**
     * 模型写完后，把 IMG_1 换成列表里的地址。
     * 配图转存请用 StockImageBatch.restore，失败的记号会被删掉，不会退回外链。
     */
    public static String restoreImageUrls(String content, List<StockImageDTO> images) {
        if (content == null || images == null || images.isEmpty()) {
            return content;
        }
        String result = content;
        for (int i = 0; i < images.size(); i++) {
            result = result.replace("IMG_" + (i + 1), images.get(i).url());
        }
        return result;
    }

    /**
     * 发给模型前去掉正文里的网址。新模型看到 https 地址会去下载并报 url error。
     */
    public static String hideUrls(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        return URL_IN_TEXT.matcher(content).replaceAll("[链接]");
    }

    /**
     * 把正文里的网址换成 KEEP_1，模型返回后再换回来。
     */
    public static String maskUrls(String content, List<String> kept) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        Matcher matcher = URL_IN_TEXT.matcher(content);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String url = matcher.group();
            int index = kept.indexOf(url);
            if (index < 0) {
                kept.add(url);
                index = kept.size() - 1;
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement("KEEP_" + (index + 1)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /**
     * 把 KEEP_1 换回掩码前的网址。从大编号往回换，避免 KEEP_1 改到 KEEP_10。
     */
    public static String unmaskUrls(String content, List<String> kept) {
        if (content == null || kept == null || kept.isEmpty()) {
            return content;
        }
        String result = content;
        for (int i = kept.size(); i >= 1; i--) {
            result = result.replace("KEEP_" + i, kept.get(i - 1));
        }
        return result;
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

    private static final class ByteArrayMultipartFile implements MultipartFile {

        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        private ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.write(dest.toPath(), content);
        }
    }
}
