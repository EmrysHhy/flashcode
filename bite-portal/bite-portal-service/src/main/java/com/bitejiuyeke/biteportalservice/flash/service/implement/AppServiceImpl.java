package com.bitejiuyeke.biteportalservice.flash.service.implement;

import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.bitecommonsecurity.domain.dto.LoginUserDTO;
import com.bitejiuyeke.bitecommonsecurity.service.TokenService;
import com.bitejiuyeke.biteportalservice.flash.constants.FlashcodeConstant;
import com.bitejiuyeke.biteportalservice.flash.domain.dto.result.AppDetailDTO;
import com.bitejiuyeke.biteportalservice.flash.domain.dto.result.GenerateAppDTO;
import com.bitejiuyeke.biteportalservice.flash.domain.entity.AppDO;
import com.bitejiuyeke.biteportalservice.flash.enums.AppTypesEnum;
import com.bitejiuyeke.biteportalservice.flash.enums.DeployStatusEnum;
import com.bitejiuyeke.biteportalservice.flash.mapper.AppMapper;
import com.bitejiuyeke.biteportalservice.flash.service.IAppService;
import com.bitejiuyeke.biteportalservice.flash.service.IGiteeService;
import com.bitejiuyeke.biteportalservice.flash.utils.AnalysisUtil;
import com.bitejiuyeke.biteportalservice.flash.utils.ChatContentSupport;
import com.bitejiuyeke.biteportalservice.flash.utils.CommandUtil;
import com.bitejiuyeke.biteportalservice.flash.utils.FileWriterUtil;
import com.github.dockerjava.api.DockerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.*;

import static com.bitejiuyeke.biteportalservice.flash.constants.FlashcodeConstant.CONTAINER_NAME;

/**
 *
 * @author Emrys
 * content:
 */
@Slf4j
@Service
public class AppServiceImpl implements IAppService {
    @Autowired
    ChatClient chatClient;
    @Autowired
    AppMapper appMapper;
    @Autowired
    TokenService tokenService;
    @Autowired
    IGiteeService giteeService;
    @Autowired
    DockerClient dockerClient;
    @Autowired
    VectorStore vectorStore;
    @Autowired
    Executor threadPoolTaskExecutor;
    @Value("${flashcode.delete-code-expire:12}")
    Integer deleteCodeExpire;

    /**
     * app应用生成
     *
     * @param appId
     * @param requirement
     * @return
     */
    @Override
    public GenerateAppDTO appGenerate(Long appId, String requirement) {
        appMapper.insertIfAbsent(appId);
        //生成代码
        String appCode = ChatContentSupport.collect(chatClient.prompt()
                .system(getSysPrompt(appId))
                .user(getUserPrompt(requirement))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(appId)))
                .advisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder().build())
                        .build()));
        log.info("生成应用代码完成，appId: {}, appCode: {}", appId, appCode);

        Map<String, String> files = AnalysisUtil.getFiles(appCode);
        AppTypesEnum appType = AnalysisUtil.resolveType(appCode, files);
        //更新数据库类型 updateType(appId, appType)
        int updated = appMapper.updateTypeById(appId, appType.getValue());
        if (updated <= 0) {
            throw new ServiceException("更新应用类型失败，app 不存在, appId=" + appId + ", appType=" + appType.getValue());
        }
        //本地代码保存
        Path codePath = FileWriterUtil.saveCode(appId, files);
        // 源码推到 Gitee flash-user-code/{appId}/，后续删本地后可再 pull
        giteeService.push(appId, files);
        // 1.根据类型编译打包    //VUE3进入 build->dist   //VUE3+Spring -> jar + dist
        // 2.html,dist,jar包保存到 /workspace/user-preview 会映射到宿主机 /deploy/dev/data/flashcodedata/flashcode-app/user-preview
        packageCode(appType, codePath, appId);
        // 3. 得到URL预览地址
        String url = FlashcodeConstant.NGINX_PRE + appId + "/#/"; //  /workspace/user-preview
        // 4. 更新数据库中的预览地址
        updated = appMapper.updateUrlById(appId, url);
        if(updated <= 0){
            log.error("更新预览地址失败，appId: {}, url: {}", appId, url);
            throw new ServiceException("更新预览地址失败");
        }
        //创建定时器,在多久以后将本地代码删除
        // 单独一个调度器（1～2 个线程就够）
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.schedule(
                () -> threadPoolTaskExecutor.execute(() -> FileWriterUtil.deleteCodeByAppId(appId)),
                deleteCodeExpire, TimeUnit.HOURS
        );


        //返回DTO
        GenerateAppDTO generateAppDTO = new GenerateAppDTO();
        generateAppDTO.setAppId(appId);
        generateAppDTO.setAppType(appType);
        generateAppDTO.setUrl(url);
        return generateAppDTO;
    }

    @Override
    public AppDetailDTO getAppDetail(Long appId) {
        AppDO app = appMapper.selectById(appId);
        if (app == null) {
            throw new ServiceException("应用不存在");
        }
        if (app.getDeployStatus() != DeployStatusEnum.DEPLOYED) {
            LoginUserDTO loginUser = tokenService.getLoginUser();
            if (loginUser == null || !"app".equals(loginUser.getUserFrom())
                    || !app.getUserId().equals(loginUser.getUserId())) {
                throw new ServiceException("无权查看该应用");
            }
        }
        AppDetailDTO appDetailDTO = new AppDetailDTO();
        appDetailDTO.setId(app.getId());
        appDetailDTO.setUserId(app.getUserId());
        appDetailDTO.setAppName(app.getAppName());
        appDetailDTO.setAppType(app.getAppType());
        appDetailDTO.setPreviewUrl(app.getAppPreviewUrl());
        return appDetailDTO;
    }

    /**
     * 打包代码并且保存到目录下
     *
     * @param appType  应用类型
     * @param loadedCode 本地代码根目录，例如 .../user-code/{appId}
     * @param appId    应用 ID
     */
    private void packageCode(AppTypesEnum appType, Path loadedCode, Long appId) {

        switch (appType) {
            case Html -> {
                // HTML 不打包，把单个 html 复制到 /workspace/user-preview/{appId}
                FileWriterUtil.copyHtmlToPreview(loadedCode, appId);
            }
            case Vue3 -> {
                // 1. 进入项目目录 loadedCode
                // 2. 执行 npm install
                CommandUtil.runCommand(FlashcodeConstant.CMD_NPM_INSTALL, loadedCode);
                // 3. 执行 npm run build
                CommandUtil.runCommand(FlashcodeConstant.CMD_NPM_BUILD, loadedCode);
                // 4. 将 dist 目录下的文件保存到指定目录
                FileWriterUtil.copyDistPreview(loadedCode, appId);
            }
            case Spring_Vue3 -> {
                // 一,前端逻辑
                // 1. 进入项目目录 /appid/frontend
                Path frontDir = loadedCode.resolve("frontend");
                // 2. 执行 npm install
                CommandUtil.runCommand(FlashcodeConstant.CMD_NPM_INSTALL, frontDir);
                // 3. 执行 npm run build
                CommandUtil.runCommand(FlashcodeConstant.CMD_NPM_BUILD, frontDir);
                // 4. 将 dist 目录下的文件保存到指定目录
                FileWriterUtil.copyDistPreview(frontDir, appId);
                // 二,后端逻辑
                // 1. 进入项目目录 /appid/backend
                Path backDir = loadedCode.resolve("backend");
                // 2. 执行 mvn clean package
                CommandUtil.runCommand(FlashcodeConstant.CMD_MVN_PACKAGE, backDir);
                // 3. 将生成的 jar 文件保存到指定目录
                Path workDir = FileWriterUtil.copyJarPreview(backDir, appId);
                // 4.启动jar包
                CommandUtil.runJar(dockerClient,workDir,appId,CONTAINER_NAME);
            }
        }
    }

    /**
     * 进一步封装用户提示词
     *
     * @param requirement
     * @return
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
