package com.bitejiuyeke.biteportalservice.flash.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.bitefileapi.file.feign.FileFeignClient;
import com.bitejiuyeke.biteportalservice.flash.agent.node.AppGenerationAgent;
import com.bitejiuyeke.biteportalservice.flash.agent.node.AppScreenshotNode;
import com.bitejiuyeke.biteportalservice.flash.agent.node.BuildPreviewNode;
import com.bitejiuyeke.biteportalservice.flash.agent.node.CommitNode;
import com.bitejiuyeke.biteportalservice.flash.agent.node.ErrorFixAgent;
import com.bitejiuyeke.biteportalservice.flash.constants.FlashcodeConstant;
import com.bitejiuyeke.biteportalservice.flash.mapper.AppMapper;
import com.bitejiuyeke.biteportalservice.flash.service.IGiteeService;
import com.github.dockerjava.api.DockerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

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
    private final IGiteeService giteeService;
    private final FileFeignClient fileFeignClient;
    private final DockerClient dockerClient;
    private final StateGraph stateGraph;
    private final Integer deleteCodeExpire;
    private final ScheduledExecutorService scheduledExecutorService;

    /** 图中注册的节点 id，与 addNode / addConditionalEdges 映射目标一致 */
    private static final String ID_APP_GENERATION_AGENT = "idAppGenerationAgent";
    private static final String ID_BUILD_PREVIEW_NODE = "idBuildPreviewNode";
    private static final String ID_ERROR_FIX_AGENT = "idErrorFixAgent";
    private static final String ID_APP_SCREENSHOT_NODE = "idAppScreenshotNode";
    private static final String ID_COMMIT_NODE = "idCommitNode";

    public MultiAgentWorkFlow(ChatClient chatClient,
                              VectorStore vectorStore,
                              AppMapper appMapper,
                              IGiteeService giteeService,
                              FileFeignClient fileFeignClient,
                              DockerClient dockerClient,
                              Executor threadPoolTaskExecutor,
                              StateGraph stateGraph,
                              Integer deleteCodeExpire,
                              ScheduledExecutorService scheduledExecutorService) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.appMapper = appMapper;
        this.giteeService = giteeService;
        this.fileFeignClient = fileFeignClient;
        this.dockerClient = dockerClient;
        this.stateGraph = stateGraph;
        this.deleteCodeExpire = deleteCodeExpire;
        this.scheduledExecutorService = scheduledExecutorService;
        addNode();
        addEdge();
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
                            ROUTE_FIX, ID_ERROR_FIX_AGENT
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

    /** 依据 {@link FlashcodeConstant#APP_IS_BUILD} */
    private String routeAfterPreview(OverAllState state) {
        if (state.value(FlashcodeConstant.APP_IS_BUILD, Boolean.class).orElse(false)) {
            return ROUTE_SCREENSHOT;
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
                    AsyncNodeAction.node_async(new AppGenerationAgent(chatClient, appMapper, vectorStore, giteeService)));
            stateGraph.addNode(ID_BUILD_PREVIEW_NODE,
                    AsyncNodeAction.node_async(new BuildPreviewNode(dockerClient, appMapper)));
            stateGraph.addNode(ID_ERROR_FIX_AGENT,
                    AsyncNodeAction.node_async(new ErrorFixAgent(chatClient, vectorStore, giteeService)));
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
