package com.bitejiuyeke.portalservice.flash.agent.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.bitejiuyeke.bitecommoncore.utils.TimestampUtil;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.portalservice.flash.constants.FlashcodeConstant;
import com.bitejiuyeke.portalservice.flash.domain.dto.result.StockImageBatch;
import com.bitejiuyeke.portalservice.flash.enums.AppTypesEnum;
import com.bitejiuyeke.portalservice.flash.mapper.AppMapper;
import com.bitejiuyeke.portalservice.flash.service.implement.ImageSearchService;
import com.bitejiuyeke.portalservice.flash.utils.AnalysisUtil;
import com.bitejiuyeke.portalservice.flash.utils.ChatContentSupport;
import com.bitejiuyeke.portalservice.flash.utils.FileWriterUtil;
import com.bitejiuyeke.portalservice.flash.utils.VisionChatSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 应用代码生成节点。
 * 规则：先按需求文档搜配图，再让模型按固定格式吐出源码；搜图失败不阻断生成。
 * 模型输出必须是第一行应用类型 + 若干 FILE: 代码块，由解析器写入 user-code/{appId}。
 */
@Slf4j
public class AppGenerationAgent implements NodeAction {
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final AppMapper appMapper;
    private final ImageSearchService imageSearchService;

    public AppGenerationAgent(ChatClient chatClient,
                              AppMapper appMapper,
                              VectorStore vectorStore,
                              ImageSearchService imageSearchService) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.appMapper = appMapper;
        this.imageSearchService = imageSearchService;
    }

    /**
     * 生成入口。
     * 规则：参考图只还原布局；网上配图由 ImageSearchService 预取后写入用户提示词；
     * 解析失败或写盘失败记 GENERATE_ERROR_MESSAGE，不在这里抛给调用方。
     */
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        int generateAttempt = state.value(FlashcodeConstant.GENERATE_ATTEMPT, Integer.class).orElse(0) + 1;
        try{
            Long appId = state.value(FlashcodeConstant.APP_ID, Long.class).orElse(null);
            String requirement = state.value(FlashcodeConstant.REQUIREMENT, String.class).orElse(null);
            if (appId == null || requirement == null || requirement.isBlank()) {
                throw new IllegalArgumentException("appId 或需求文档为空");
            }
            Path image = VisionChatSupport.resolveImage(
                    state.value(FlashcodeConstant.REFERENCE_PATH, String.class).orElse(null));
            StockImageBatch stockImages = imageSearchService.searchForRequirement(appId, requirement);
            log.info("搜图结果 appId={}, count={}", appId, stockImages.hints().size());
            String appCode = generateCode(appId,
                    state.value(FlashcodeConstant.USER_ID, Long.class).orElse(null),
                    requirement, image, stockImages);
            log.info("生成应用代码完成，appId: {}", appId);

            Map<String, String> files = AnalysisUtil.getFiles(appCode);
            AppTypesEnum appType = AnalysisUtil.resolveType(appCode, files);
            //更新数据库类型 updateType(appId, appType)
            int updated = appMapper.updateTypeById(appId, appType.getValue());
            if (updated <= 0) {
                throw new ServiceException("更新应用类型失败，app 不存在, appId=" + appId + ", appType=" + appType.getValue());
            }
            //本地代码保存
            Path codePath = FileWriterUtil.saveCode(appId, files);

            //AppType
            //CodePath
            Map<String, Object> result = new HashMap<>();
            result.put(FlashcodeConstant.APP_TYPE, appType.name());
            result.put(FlashcodeConstant.CODE_PATH, codePath.toString());
            result.put(FlashcodeConstant.APP_IS_GENERATE, Boolean.TRUE);
            result.put(FlashcodeConstant.FILES, files);
            result.put(FlashcodeConstant.GENERATE_ATTEMPT, generateAttempt);
            return result;
        }catch (Exception e){
            log.error("生成应用代码失败，异常信息: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put(FlashcodeConstant.APP_IS_GENERATE, Boolean.FALSE);
            result.put(FlashcodeConstant.GENERATE_ERROR_MESSAGE, e.getMessage());
            result.put(FlashcodeConstant.GENERATE_ATTEMPT, generateAttempt);
            return result;
        }

    }

    /**
     * 调用大模型生成源码。
     * 规则：不把搜图工具挂到 ChatClient，避免 stream 过程中调工具拿不到结果；
     * 有参考图时切视觉模型；输出必须能被 AnalysisUtil 解析。
     */
    private String generateCode(Long appId, Long userId, String requirement, Path image,
                               StockImageBatch stockImages) {
        long beginSeconds = TimestampUtil.getCurrentSeconds();
        boolean hasStock = stockImages != null && !stockImages.isEmpty();
        var spec = chatClient.prompt()
                .system(getSysPrompt(appId, hasStock))
                .user(u -> {
                    u.text(getUserPrompt(requirement, image != null, stockImages));
                    VisionChatSupport.attachImage(u, image);
                })
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(appId))
                        .param(FlashcodeConstant.USER_ID, userId)
                        .param(FlashcodeConstant.APP_ID, appId))
                .advisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder().build())
                        .build());
        if (image != null) {
            log.info("带参考图生成，使用当前对话模型, appId={}", appId);
        }
        long endSeconds = TimestampUtil.getCurrentSeconds();
        long differenceSeconds = TimestampUtil.calculateDifferenceSeconds(beginSeconds, endSeconds);
        log.info("生成应用代码耗时: {} 秒", differenceSeconds);
        return stockImages.restore(ChatContentSupport.collect(spec));
    }

    /**
     * 用户提示词。
     * 规则：正文是需求文档；有用户上传参考图时附加【参考图】说明；
     * 有搜到的配图时只给 IMG_1 记号，模型写完后再换成 OSS 地址。
     */
    private String getUserPrompt(String requirement, boolean hasImage, StockImageBatch stockImages) {
        String prompt = String.join("\n",
                "【用户需求文档】 ",
                ImageSearchService.hideUrls(requirement),
                "【输出要求】请严格按照系统提示的格式输出，不要添加多余解释。 "
        );
        if (hasImage) {
            prompt += "\n【参考图】请根据用户上传的参考图还原布局、配色与主要模块；与需求文档冲突时以需求文档为准。\n";
        }
        prompt += ImageSearchService.imageHint(stockImages == null ? null : stockImages.hints());
        return prompt;
    }
    /**
     * 系统提示词。
     * 规则：只能选 Html / Vue3 / Spring_Vue3；禁止复杂鉴权和外部存储；
     * 有配图必须用「可用图片」里的 IMG_n，没有则用色块/SVG，禁止编造地址；
     * 输出第一行是类型，随后每个文件 FILE: 相对路径 + 完整代码块，禁止省略。
     */
    private String getSysPrompt(Long appId, boolean hasStockImages) {
        return String.join("\n",
                "你是资深全栈工程师和架构师，精通现代 Web 开发。你的目标是严格依据用户需求文档生成完整、可运行、代码整洁且页面美观的应用代码。",
                "### 应用类型决策",
                "根据用户需求文档选择最合适的一种应用类型进行生成，注意仅可选择以下三种应用类型",
                "1. **" + AppTypesEnum.Html.name() + "**：用户明确指出或需求简单，仅需展示或简单交互。",
                "2. **" + AppTypesEnum.Vue3.name() + "**：用户明确指出或需求涉及复杂交互、多页面路由或组件化开发，但无需后端服务。",
                "3. **" + AppTypesEnum.Spring_Vue3.name() + "**：用户明确指出或需求文档中明确需要后端逻辑。",
                "### 通用生成规范",
                "- **复杂逻辑**：生成的所有应用不要包含复杂逻辑（例如：身份认证等）。",
                "- **数据存储**：生成的所有应用数据存储不依赖任何第三方存储机制。",
                hasStockImages
                        ? "- **配图**：按「可用图片」冒号前的主体选用对应的 IMG_n。一项内容只用主体一致的那张图。主体对不上就用色块或 SVG，禁止编造网址，禁止把界面截图当成产品图。"
                        : "- **配图**：没有可用图片时用 CSS 色块或 SVG 绘制，禁止编造无法访问的图片地址。",
                "### 类型详细规范",
                "#### 1. 单个 HTML 页面（" + AppTypesEnum.Html.name() + "）",
                "- **结构**：仅输出一个 `index.html` 文件。",
                "- **技术**：只能使用 HTML、CSS 和原生 JavaScript。禁止引入外部 CSS/JS 库（如 Bootstrap，jQuery）。",
                "- **实现**：CSS 必须内联在 `<head><style>` 中；JS 必须内联在 `</body>` 前的 `<script>` 中。",
                "#### 2. Vue3 工程（" + AppTypesEnum.Vue3.name() + "）",
                "- **技术栈**：Vue 3 (Composition API，`<script setup>`)，Vite，Vue Router 4.x。",
                "- **文件结构**：必须包含标准工程结构（`package.json`，`vite.config.js`，`index.html`，`src/main.js`，`src/App.vue` 等）。",
                "- **配置强制要求**：",
                "  - `vite.config.js`：必须配置 `base: './'`，配置 `@` 别名指向 `./src`。若用 `path.resolve`，必须先 `import path from 'node:path'` 或 `const path = require('path')`，禁止直接使用未定义的 `path`。",
                "  - `router`：必须使用 `createWebHashHistory()`。",
                "  - `package.json`：必须包含 `dev` (`vite`) 和 `build` (`vite build`) 脚本。依赖写死版本、禁止 `^`：vue `3.3.11`，vue-router `4.2.5`，vite `4.5.2`，@vitejs/plugin-vue `4.5.2`。",
                "  - `index.html`：禁止空文件。必须是完整 Vite 入口 HTML，至少包含 `<div id=\"app\"></div>` 和 `<script type=\"module\" src=\"/src/main.js\"></script>`。",
                "- **质量保证**：",
                "  - 必须能够通过 `npm install` 安装项目所需依赖，并且能够通过 `npm run build` 正确完成构建生成dist目录",
                "#### 3. SpringBoot + Vue3 工程（" + AppTypesEnum.Spring_Vue3.name() + "）",
                "- **目录结构**：前端代码置于 `frontend/` 目录下，后端代码置于 `backend/` 目录下。",
                "- **前端部分（frontend/）**：",
                "  - 遵循上述 **" + AppTypesEnum.Vue3.name() + "** 的所有规范。",
                "  - **API 请求关键**：前端请求后端接口时，URL **必须**统一添加前缀 `/" + appId + "/api`（例如 `/" + appId + "/api/users`）。这是网关转发规则，务必遵守。",
                "- **后端部分（backend/）**：",
                "  - **技术栈**：Spring Boot 3.x、JDK 21、Maven3.9。",
                "  - **代码规范**：务必通过java自身语法完成代码不要引入其它资源",
                "  - **文件结构**：必须包含标准工程结构（`pom.xml`，`xxxApplication.java(启动类)` 等）。",
                "  - **核心依赖**：`pom.xml` 必须继承 `spring-boot-starter-parent`，引入 `spring-boot-starter-web`。",
                "  - **构建配置**：`pom.xml` 必须包含 `spring-boot-maven-plugin` 以支持 `java -jar` 运行。",
                "  - **代码实现**：所有 Controller 的 `@RequestMapping` 必须以 `/api` 开头（例如 `@RequestMapping(\"/api/users\")`）。注意此处不加：" + appId,
                "  - **数据存储**：**严禁**依赖 MySQL/Redis 等外部服务。仅使用内存（`ConcurrentHashMap`）或本地文件模拟数据库。",
                "  - **启动类**：必须包含标准的 SpringBoot 启动类。",
                "- **质量保证**：",
                "  - 必须能够通过 `mvn clean package -DskipTests`生成jar，并且能够通过 `java -jar`正确启动jar包",
                "### 输出格式约束（CRITICAL）",
                "你必须严格按照以下格式输出，解析器依赖此格式：",
                "1. **第一行**：特别注意必须在第一行输出生成应用的类类型（例如：" + AppTypesEnum.promptAppTypes() + "）。",
                "2. **文件内容**：紧接着按以下格式输出每个文件：",
                "FILE: <relative_path>",
                "```<language>",
                "<complete_file_content>",
                "```",
                "  - `<relative_path>`：文件的相对路径（如 `index.html`，`frontend/src/App.vue`，`backend/src/main/resources/application.properties`）。",
                "  - `<complete_file_content>`：**完整**的文件内容，**绝对禁止**空代码块、省略、使用占位符或 `// ...`。"
        );
    }
}
