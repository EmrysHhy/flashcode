package com.bitejiuyeke.portalservice.flash.agent;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.bitecommonsecurity.domain.dto.LoginUserDTO;
import com.bitejiuyeke.bitecommonsecurity.service.TokenService;
import com.bitejiuyeke.bitefileapi.file.feign.FileFeignClient;
import com.bitejiuyeke.portalservice.flash.agent.node.AppGenerationAgent;
import com.bitejiuyeke.portalservice.flash.agent.node.AppScreenshotNode;
import com.bitejiuyeke.portalservice.flash.agent.node.BuildPreviewNode;
import com.bitejiuyeke.portalservice.flash.agent.node.CommitNode;
import com.bitejiuyeke.portalservice.flash.agent.node.ErrorFixAgent;
import com.bitejiuyeke.portalservice.flash.constants.FlashcodeConstant;
import com.bitejiuyeke.portalservice.flash.domain.dto.result.GenerateAppDTO;
import com.bitejiuyeke.portalservice.flash.enums.AppTypesEnum;
import com.bitejiuyeke.portalservice.flash.mapper.AppMapper;
import com.bitejiuyeke.portalservice.flash.service.IGiteeService;
import com.bitejiuyeke.portalservice.flash.service.implement.ImageSearchService;
import com.bitejiuyeke.portalservice.flash.utils.FileWriterUtil;
import com.github.dockerjava.api.DockerClient;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;

/**
 * Flash 应用生成的多节点工作流（StateGraph）。
 * <p>
 * 主路径：生成代码 → 构建预览 → 截图 → 推 Gitee 并收尾。
 * 失败时按条件边分支；gen / screenshot / commit 各自最多执行 {@link #MAX_ATTEMPT} 次，超限则 END。
 * <p>
 * 流程示意：
 * <pre>
 * START → gen ──成功──→ pre ──成功──→ screenshot ──成功──→ commit ──成功──→ END
 *          │              │
 *          │失败(<2次)     │失败
 *          └→ gen         └→ fix ──成功──→ pre
 *          │失败(≥2次)              │
 *          END                    │失败且 gen&lt;2次 → gen
 *                                 │失败且 gen≥2次 → END
 * screenshot / commit 失败：未达次数上限则重试本节点，否则 END
 * </pre>
 * 路由依据各节点写入 state 的标志位（如 {@link FlashcodeConstant#APP_IS_GENERATE}）及 attempt 计数。
 *
 * @author Emrys
 */
@Slf4j
public class MultiAgentWorkFlow {

    /** gen、screenshot、commit 在单次工作流内允许的最大执行次数 */
    private static final int MAX_ATTEMPT = 2;

    /** 条件边返回值，需与 {@link #addEdge()} 中 Map 的 key 一致 */
    private static final String ROUTE_PREVIEW = "preview";
    private static final String ROUTE_RETRY = "retry";
    private static final String ROUTE_FIX = "fix";
    private static final String ROUTE_GEN = "gen";
    private static final String ROUTE_SCREENSHOT = "screenshot";
    private static final String ROUTE_COMMIT = "commit";
    private static final String ROUTE_END = "end";

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final AppMapper appMapper;
    private final TokenService tokenService;
    private final IGiteeService giteeService;
    private final FileFeignClient fileFeignClient;
    private final DockerClient dockerClient;
    private final StateGraph stateGraph;
    private final CompiledGraph compiledGraph;
    private final Integer deleteCodeExpire;
    private final String nginxPre;
    private final ScheduledExecutorService scheduledExecutorService;
    private final MeterRegistry meterRegistry;
    private final ImageSearchService imageSearchService;
    /** 每个 appId 最近一次生成耗时（秒），供 Prometheus 仪表读取。 */
    private final ConcurrentHashMap<String, AtomicReference<Double>> lastGenerateSeconds = new ConcurrentHashMap<>();

    /** 图中注册的节点 id，与 addNode / addConditionalEdges 映射目标一致 */
    private static final String ID_APP_GENERATION_AGENT = "idAppGenerationAgent";
    private static final String ID_BUILD_PREVIEW_NODE = "idBuildPreviewNode";
    private static final String ID_ERROR_FIX_AGENT = "idErrorFixAgent";
    private static final String ID_APP_SCREENSHOT_NODE = "idAppScreenshotNode";
    private static final String ID_COMMIT_NODE = "idCommitNode";

    public MultiAgentWorkFlow(ChatClient chatClient,
                              VectorStore vectorStore,
                              AppMapper appMapper,
                              TokenService tokenService,
                              IGiteeService giteeService,
                              FileFeignClient fileFeignClient,
                              DockerClient dockerClient,
                              Integer deleteCodeExpire,
                              String nginxPre,
                              ScheduledExecutorService scheduledExecutorService,
                              MeterRegistry meterRegistry,
                              ImageSearchService imageSearchService) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.appMapper = appMapper;
        this.tokenService = tokenService;
        this.giteeService = giteeService;
        this.fileFeignClient = fileFeignClient;
        this.dockerClient = dockerClient;
        this.deleteCodeExpire = deleteCodeExpire;
        this.nginxPre = nginxPre;
        this.scheduledExecutorService = scheduledExecutorService;
        this.meterRegistry = meterRegistry;
        this.imageSearchService = imageSearchService;
        this.stateGraph = new StateGraph(keyStrategyFactory());
        addNode();
        addEdge();
        try {
            this.compiledGraph = stateGraph.compile();
        } catch (GraphStateException e) {
            log.error("编译工作流失败", e);
            throw new ServiceException("编译工作流失败：" + e.getMessage());
        }
    }

    /**
     * 单次请求一份新 Map；图已在启动时 compile，这里只 invoke。
     */
    public GenerateAppDTO generate(Long appId, String requirement, MultipartFile reference) {
        if (appId == null || requirement == null || requirement.isBlank()) {
            throw new ServiceException("appId 和需求文档不能为空");
        }
        LoginUserDTO loginUser = tokenService.getLoginUser();
        Long ownerId = loginUser == null ? null : loginUser.getUserId();
        appMapper.insertIfAbsent(appId, ownerId);
        if (ownerId != null) {
            appMapper.bindOwnerIfPlaceholder(appId, ownerId);
        }
        Map<String, Object> input = new HashMap<>();
        input.put(FlashcodeConstant.APP_ID, appId);
        input.put(FlashcodeConstant.USER_ID, ownerId);
        input.put(FlashcodeConstant.REQUIREMENT, requirement);
        input.put(FlashcodeConstant.GENERATE_ATTEMPT, 0);
        input.put(FlashcodeConstant.SCREENSHOT_ATTEMPT, 0);
        input.put(FlashcodeConstant.COMMIT_ATTEMPT, 0);
        if (reference != null && !reference.isEmpty()) {
            log.info("参考文件：{}，大小：{} 字节", reference.getOriginalFilename(), reference.getSize());
            Path path = FileWriterUtil.writeReferenceFile(appId, reference);
            input.put(FlashcodeConstant.REFERENCE_PATH, path.toAbsolutePath().toString());
        }

        RunnableConfig config = RunnableConfig.builder()
                .threadId(String.valueOf(appId))
                .build();
        long startedAt = System.nanoTime();
        String outcome = "failure";
        try {
            OverAllState state = compiledGraph.invoke(input, config)
                    .orElseThrow(() -> new ServiceException("工作流未返回状态"));

            if (!state.value(FlashcodeConstant.APP_IS_COMMIT, Boolean.class).orElse(false)) {
                String error = state.value(FlashcodeConstant.COMMIT_ERROR_MESSAGE, String.class)
                        .orElseGet(() -> state.value(FlashcodeConstant.SCREENSHOT_ERROR_MESSAGE, String.class)
                                .orElseGet(() -> state.value(FlashcodeConstant.FIX_ERROR_MESSAGE, String.class)
                                        .orElseGet(() -> state.value(FlashcodeConstant.BUILD_ERROR_MESSAGE, String.class)
                                                .orElseGet(() -> state.value(FlashcodeConstant.GENERATE_ERROR_MESSAGE, String.class)
                                                        .orElse("应用生成失败")))));
                throw new ServiceException(error);
            }

            GenerateAppDTO dto = new GenerateAppDTO();
            dto.setAppId(appId);
            dto.setAppType(AppTypesEnum.of(state.value(FlashcodeConstant.APP_TYPE, String.class).orElse(null)));
            dto.setUrl(state.value(FlashcodeConstant.PREVIEW_URL, String.class).orElse(null));
            outcome = "success";
            return dto;
        } finally {
            recordGenerateTime(appId, startedAt, outcome);
        }
    }

    /**
     * 按 appId 记录本次生成耗时。仪表是最近一次秒数，计时器用于累计次数和平均耗时。
     */
    private void recordGenerateTime(Long appId, long startedAtNanos, String outcome) {
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAtNanos);
        double seconds = elapsed.toNanos() / 1_000_000_000.0;
        Tags tags = Tags.of(FlashcodeConstant.APP_ID, String.valueOf(appId));
        Timer.builder("app_generate")
                .description("App generation duration by appId")
                .tags(tags)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(elapsed);
        AtomicReference<Double> holder = lastGenerateSeconds.computeIfAbsent(String.valueOf(appId), ignored -> {
            AtomicReference<Double> created = new AtomicReference<>(seconds);
            Gauge.builder("app_generate_last_seconds", created, AtomicReference::get)
                    .description("Latest app generation duration in seconds by appId")
                    .tags(tags)
                    .register(meterRegistry);
            return created;
        });
        holder.set(seconds);
        log.info("应用生成耗时, appId={}, outcome={}, seconds={}", appId, outcome, seconds);
    }

    /** 节点回写的字段一律覆盖，不要 Append */
    private static KeyStrategyFactory keyStrategyFactory() {
        return () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            ReplaceStrategy replace = new ReplaceStrategy();
            strategies.put(FlashcodeConstant.APP_ID, replace);
            strategies.put(FlashcodeConstant.USER_ID, replace);
            strategies.put(FlashcodeConstant.REQUIREMENT, replace);
            strategies.put(FlashcodeConstant.FILES, replace);
            strategies.put(FlashcodeConstant.APP_IS_GENERATE, replace);
            strategies.put(FlashcodeConstant.APP_IS_BUILD, replace);
            strategies.put(FlashcodeConstant.APP_IS_FIX, replace);
            strategies.put(FlashcodeConstant.APP_IS_SCREENSHOT, replace);
            strategies.put(FlashcodeConstant.APP_IS_COMMIT, replace);
            strategies.put(FlashcodeConstant.CODE_PATH, replace);
            strategies.put(FlashcodeConstant.APP_TYPE, replace);
            strategies.put(FlashcodeConstant.ERROR_TYPE, replace);
            strategies.put(FlashcodeConstant.GENERATE_ERROR_MESSAGE, replace);
            strategies.put(FlashcodeConstant.PHOTO_PATH, replace);
            strategies.put(FlashcodeConstant.BUILD_ERROR_MESSAGE, replace);
            strategies.put(FlashcodeConstant.FIX_ERROR_MESSAGE, replace);
            strategies.put(FlashcodeConstant.SCREENSHOT_ERROR_MESSAGE, replace);
            strategies.put(FlashcodeConstant.COMMIT_ERROR_MESSAGE, replace);
            strategies.put(FlashcodeConstant.GENERATE_ATTEMPT, replace);
            strategies.put(FlashcodeConstant.SCREENSHOT_ATTEMPT, replace);
            strategies.put(FlashcodeConstant.COMMIT_ATTEMPT, replace);
            strategies.put(FlashcodeConstant.PREVIEW_URL, replace);
            strategies.put(FlashcodeConstant.ERROR_FIXABLE, replace);
            strategies.put(FlashcodeConstant.REFERENCE_PATH, replace);
            return strategies;
        };
    }

    /**
     * 注册边：仅 START→gen 为无条件边，其余均为条件边（由 routeAfter* 读 state 决定下一跳）。
     */
    private void addEdge() {
        try {
            stateGraph.addEdge(StateGraph.START, ID_APP_GENERATION_AGENT);

            // gen：成功去预览；失败且未达次数上限则再生成；否则结束
            stateGraph.addConditionalEdges(ID_APP_GENERATION_AGENT,
                    edge_async(this::routeAfterGenerate),
                    Map.of(
                            ROUTE_PREVIEW, ID_BUILD_PREVIEW_NODE,
                            ROUTE_RETRY, ID_APP_GENERATION_AGENT,
                            ROUTE_END, StateGraph.END
                    ));

            // pre：成功去截图；失败去 fix（预览节点本身不重试）
            stateGraph.addConditionalEdges(ID_BUILD_PREVIEW_NODE,
                    edge_async(this::routeAfterPreview),
                    Map.of(
                            ROUTE_SCREENSHOT, ID_APP_SCREENSHOT_NODE,
                            ROUTE_FIX, ID_ERROR_FIX_AGENT,
                            ROUTE_END, StateGraph.END
                    ));

            // fix：成功回预览；失败且整次流程 gen 次数未满则重新生成；否则结束
            stateGraph.addConditionalEdges(ID_ERROR_FIX_AGENT,
                    edge_async(this::routeAfterFix),
                    Map.of(
                            ROUTE_PREVIEW, ID_BUILD_PREVIEW_NODE,
                            ROUTE_GEN, ID_APP_GENERATION_AGENT,
                            ROUTE_END, StateGraph.END
                    ));

            // screenshot：成功去 commit；失败可重试本节点或结束
            stateGraph.addConditionalEdges(ID_APP_SCREENSHOT_NODE,
                    edge_async(this::routeAfterScreenshot),
                    Map.of(
                            ROUTE_COMMIT, ID_COMMIT_NODE,
                            ROUTE_RETRY, ID_APP_SCREENSHOT_NODE,
                            ROUTE_END, StateGraph.END
                    ));

            // commit：成功结束；失败可重试本节点或结束
            stateGraph.addConditionalEdges(ID_COMMIT_NODE,
                    edge_async(this::routeAfterCommit),
                    Map.of(
                            ROUTE_END, StateGraph.END,
                            ROUTE_RETRY, ID_COMMIT_NODE
                    ));
        } catch (GraphStateException e) {
            log.error("初始化工作流边失败", e);
            throw new ServiceException("初始化工作流边失败：" + e.getMessage());
        }
    }

    /** 依据 {@link FlashcodeConstant#APP_IS_GENERATE} 与 {@link FlashcodeConstant#GENERATE_ATTEMPT} */
    private String routeAfterGenerate(OverAllState state) {
        if (state.value(FlashcodeConstant.APP_IS_GENERATE, Boolean.class).orElse(false)) {
            return ROUTE_PREVIEW;
        }
        int attempt = state.value(FlashcodeConstant.GENERATE_ATTEMPT, Integer.class).orElse(0);
        if (attempt < MAX_ATTEMPT) {
            return ROUTE_RETRY;
        }
        return ROUTE_END;
    }

    /** 构建成功去截图；改代码可修则 fix；Docker/改库等环境问题直接结束 */
    private String routeAfterPreview(OverAllState state) {
        if (state.value(FlashcodeConstant.APP_IS_BUILD, Boolean.class).orElse(false)) {
            return ROUTE_SCREENSHOT;
        }
        if (!state.value(FlashcodeConstant.ERROR_FIXABLE, Boolean.class).orElse(true)) {
            return ROUTE_END;
        }
        return ROUTE_FIX;
    }

    /** 依据 {@link FlashcodeConstant#APP_IS_FIX}；fix 失败回流 gen 时仍受 gen 总次数上限约束 */
    private String routeAfterFix(OverAllState state) {
        if (state.value(FlashcodeConstant.APP_IS_FIX, Boolean.class).orElse(false)) {
            return ROUTE_PREVIEW;
        }
        int generateAttempt = state.value(FlashcodeConstant.GENERATE_ATTEMPT, Integer.class).orElse(0);
        if (generateAttempt < MAX_ATTEMPT) {
            return ROUTE_GEN;
        }
        return ROUTE_END;
    }

    /** 依据 {@link FlashcodeConstant#APP_IS_SCREENSHOT} 与 {@link FlashcodeConstant#SCREENSHOT_ATTEMPT} */
    private String routeAfterScreenshot(OverAllState state) {
        if (state.value(FlashcodeConstant.APP_IS_SCREENSHOT, Boolean.class).orElse(false)) {
            return ROUTE_COMMIT;
        }
        int attempt = state.value(FlashcodeConstant.SCREENSHOT_ATTEMPT, Integer.class).orElse(0);
        if (attempt < MAX_ATTEMPT) {
            return ROUTE_RETRY;
        }
        return ROUTE_END;
    }

    /** 依据 {@link FlashcodeConstant#APP_IS_COMMIT} 与 {@link FlashcodeConstant#COMMIT_ATTEMPT} */
    private String routeAfterCommit(OverAllState state) {
        if (state.value(FlashcodeConstant.APP_IS_COMMIT, Boolean.class).orElse(false)) {
            return ROUTE_END;
        }
        int attempt = state.value(FlashcodeConstant.COMMIT_ATTEMPT, Integer.class).orElse(0);
        if (attempt < MAX_ATTEMPT) {
            return ROUTE_RETRY;
        }
        return ROUTE_END;
    }

    /** 注册各 NodeAction；节点非 Spring Bean，依赖由本类构造器注入后 new 传入 */
    private void addNode() {
        try {
            stateGraph.addNode(ID_APP_GENERATION_AGENT,
                    AsyncNodeAction.node_async(new AppGenerationAgent(chatClient, appMapper, vectorStore, imageSearchService)));
            stateGraph.addNode(ID_BUILD_PREVIEW_NODE,
                    AsyncNodeAction.node_async(new BuildPreviewNode(dockerClient, appMapper, nginxPre)));
            stateGraph.addNode(ID_ERROR_FIX_AGENT,
                    AsyncNodeAction.node_async(new ErrorFixAgent(chatClient)));
            stateGraph.addNode(ID_APP_SCREENSHOT_NODE,
                    AsyncNodeAction.node_async(new AppScreenshotNode(appMapper, fileFeignClient)));
            stateGraph.addNode(ID_COMMIT_NODE,
                    AsyncNodeAction.node_async(new CommitNode(giteeService, deleteCodeExpire, scheduledExecutorService)));
        } catch (GraphStateException e) {
            log.error("初始化工作流节点失败", e);
            throw new ServiceException("初始化工作流节点失败：" + e.getMessage());
        }
    }
}
