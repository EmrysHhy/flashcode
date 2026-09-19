package com.bitejiuyeke.biteportalservice.flash.agent.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.bitejiuyeke.biteportalservice.flash.constants.FlashcodeConstant;
import com.bitejiuyeke.biteportalservice.flash.enums.AppTypesEnum;
import com.bitejiuyeke.biteportalservice.flash.service.IGiteeService;
import com.bitejiuyeke.biteportalservice.flash.utils.AnalysisUtil;
import com.bitejiuyeke.biteportalservice.flash.utils.FileWriterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;

import java.nio.file.Path;
import java.util.Map;

/**
 *
 * @author Emrys
 * content:
 */
@Slf4j
public class ErrorFixAgent implements NodeAction {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public ErrorFixAgent(ChatClient chatClient,
                              VectorStore vectorStore,
                              IGiteeService giteeService) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Long appId = state.value(FlashcodeConstant.APP_ID, Long.class).orElse(null);

        try {
            if(!state.value(FlashcodeConstant.APP_IS_BUILD,Boolean.class).orElse(false)){
                log.warn("应用构建失败，appId: {}", appId);
                String errorType = state.value(FlashcodeConstant.ERROR_TYPE, String.class).orElse(null);
                String errorMessage = state.value(FlashcodeConstant.BUILD_ERROR_MESSAGE, String.class).orElse(null);
                String requirement = state.value(FlashcodeConstant.REQUIREMENT, String.class).orElse(null); //需求文档
                String appTypeName = state.value(FlashcodeConstant.APP_TYPE, String.class).orElse(null); // 类型
                Map<String, String> files = state.value(FlashcodeConstant.FILES, Map.class).orElse(null); // 错误代码
                //修复代码中
                String appCode  = fixError(appId, errorType, errorMessage, requirement, appTypeName,files);   //大模型修改
                log.info("修复应用代码，appId: {}, appCode: {}", appId, appCode);

                Map<String, String> newFiles = AnalysisUtil.getFiles(appCode); // 解析出文件
                //本地代码保存
                Path codePath = FileWriterUtil.saveCode(appId, newFiles);
                //CodePath
                return Map.of(
                        FlashcodeConstant.CODE_PATH, codePath.toString(),
                        FlashcodeConstant.APP_IS_FIX, Boolean.TRUE,
                        FlashcodeConstant.FILES, newFiles
                );
            }else{
                log.warn("应用构建成功，无需修复，appId: {}", appId);
                return Map.of(FlashcodeConstant.APP_IS_FIX, Boolean.TRUE);
            }
        }catch (Exception e){
            log.error("修改代码异常，异常信息: {}", e.getMessage(), e);      //工作流应该走到哪里?  是代码错误?还是业务错误?
            return Map.of(
                    FlashcodeConstant.APP_IS_FIX, Boolean.FALSE,
                    FlashcodeConstant.FIX_ERROR_MESSAGE, e.getMessage()
            );
        }

    }

    /**
     * 修复错误
     * @param appId
     * @param errorType
     * @param errorMessage
     * @param requirement
     * @param appTypeName
     * @param files
     */
    private String fixError(Long appId, String errorType, String errorMessage, String requirement, String appTypeName,Map<String,String> files) {
        String res = chatClient.prompt()
                .call()
                .content();
        return res;
    }

    /**
     * 进一步封装用户提示词
     * @param requirement 用户需求
     * @return 封装后的用户提示词
     */
    private String getUserPrompt(String requirement) {
        return String.join("\n",
                "【用户需求文档】 ",
                requirement,
                "【输出要求】请严格按照系统提示的格式输出，不要添加多余解释。 "
        );
    }
    /**
     * 系统提示词
     */
    private String getSysPrompt(Long appId) {
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
                "### 类型详细规范",
                "#### 1. 单个 HTML 页面（" + AppTypesEnum.Html.name() + "）",
                "- **结构**：仅输出一个 `index.html` 文件。",
                "- **技术**：只能使用 HTML、CSS 和原生 JavaScript。禁止引入外部 CSS/JS 库（如 Bootstrap，jQuery）。",
                "- **实现**：CSS 必须内联在 `<head><style>` 中；JS 必须内联在 `</body>` 前的 `<script>` 中。",
                "#### 2. Vue3 工程（" + AppTypesEnum.Vue3.name() + "）",
                "- **技术栈**：Vue 3 (Composition API，`<script setup>`)，Vite，Vue Router 4.x。",
                "- **文件结构**：必须包含标准工程结构（`package.json`，`vite.config.js`，`index.html`，`src/main.js`，`src/App.vue` 等）。",
                "- **配置强制要求**：",
                "  - `vite.config.js`：必须配置 `base: './'`，配置 `@` 别名指向 `./src`。",
                "  - `router`：必须使用 `createWebHashHistory()`。",
                "  - `package.json`：必须包含 `dev` (`vite`) 和 `build` (`vite build`) 脚本。",
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
