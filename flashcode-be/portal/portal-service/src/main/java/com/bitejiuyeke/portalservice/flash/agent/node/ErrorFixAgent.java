package com.bitejiuyeke.portalservice.flash.agent.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.bitejiuyeke.portalservice.flash.constants.FlashcodeConstant;
import com.bitejiuyeke.portalservice.flash.enums.AppTypesEnum;
import com.bitejiuyeke.portalservice.flash.utils.AnalysisUtil;
import com.bitejiuyeke.portalservice.flash.utils.ChatContentSupport;
import com.bitejiuyeke.portalservice.flash.utils.FileWriterUtil;
import com.bitejiuyeke.portalservice.flash.utils.VisionChatSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author Emrys
 * content:
 */
@Slf4j
public class ErrorFixAgent implements NodeAction {

    private final ChatClient chatClient;

    public ErrorFixAgent(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Long appId = state.value(FlashcodeConstant.APP_ID, Long.class).orElse(null);
        log.info("开始修复应用代码，appId: {}", appId);
        try {
            if(!state.value(FlashcodeConstant.APP_IS_BUILD,Boolean.class).orElse(false)){
                log.warn("应用构建失败，appId: {}", appId);
                String errorType = state.value(FlashcodeConstant.ERROR_TYPE, String.class).orElse(FlashcodeConstant.STAGE_UNKNOWN);
                String errorMessage = state.value(FlashcodeConstant.BUILD_ERROR_MESSAGE, String.class).orElse(null);
                boolean fixable = state.value(FlashcodeConstant.ERROR_FIXABLE, Boolean.class).orElse(true);
                if (!fixable) {
                    log.warn("失败阶段不可用改代码修复，跳过模型, appId={}, stage={}", appId, errorType);
                    return Map.of(
                            FlashcodeConstant.APP_IS_FIX, Boolean.FALSE,
                            FlashcodeConstant.FIX_ERROR_MESSAGE, "阶段 " + errorType + " 属于环境/发布问题，不调用模型修代码"
                    );
                }
                String requirement = state.value(FlashcodeConstant.REQUIREMENT, String.class).orElse(null);
                String appTypeName = state.value(FlashcodeConstant.APP_TYPE, String.class).orElse(null);
                Map<String, String> files = FileWriterUtil.readSourceFiles(
                        state.value(FlashcodeConstant.CODE_PATH, String.class).orElse(null));
                Path image = VisionChatSupport.resolveImage(
                        state.value(FlashcodeConstant.REFERENCE_PATH, String.class).orElse(null));
                String appCode = fixError(appId,
                        state.value(FlashcodeConstant.USER_ID, Long.class).orElse(null),
                        errorType, errorMessage, requirement, appTypeName, files, image);
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

    private String fixError(Long appId, Long userId, String errorType, String errorMessage, String requirement, String appTypeName,
                            Map<String, String> files, Path image) {
        Map<String, String> currentFiles = files == null ? Map.of() : files;
        var spec = chatClient.prompt()
                .system(fixSystemPrompt())
                .user(u -> {
                    u.text(fixUserPrompt(errorMessage, errorType, currentFiles, appTypeName, requirement, image != null));
                    VisionChatSupport.attachImage(u, image);
                })
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(appId))
                        .param(FlashcodeConstant.USER_ID, userId)
                        .param(FlashcodeConstant.APP_ID, appId));
        if (image != null) {
            log.info("带参考图修复，使用当前对话模型, stage={}", errorType);
        }
        return ChatContentSupport.collect(spec);
    }

    /**
     * 用户提示词
     * @param errorMessage 错误信息
     * @param errorType 错误类型
     * @param currentFiles 当前文件
     * @param appType 应用类型
     * @param appDoc 应用文档
     * @return 封装后的用户提示词
     */
    private String fixUserPrompt(String errorMessage, String errorType,
                                      Map<String, String> currentFiles, String appType,
                                      String appDoc, boolean hasImage) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("### 错误信息\n");
        prompt.append("**失败阶段**: ").append(errorType).append("（npm_install / npm_build / mvn_package / copy_preview）\n");
        prompt.append("**构建输出（原文，请据此定位文件和行）**:\n");
        prompt.append("```\n").append(errorMessage == null ? "" : errorMessage).append("\n```\n\n");

        prompt.append("### 原始需求\n");
        prompt.append(appDoc).append("\n\n");

        prompt.append("### 当前代码\n");
        prompt.append("应用类型: ").append(appType).append("\n");
        prompt.append("文件数量: ").append(currentFiles.size()).append("\n\n");

        // 只包含关键文件内容，避免提示词过长
        Map<String, String> keyFiles = filterKeyFiles(currentFiles, appType);
        for (Map.Entry<String, String> entry : keyFiles.entrySet()) {
            prompt.append("FILE: ").append(entry.getKey()).append("\n");
            prompt.append("```\n").append(entry.getValue()).append("\n```\n\n");
        }

        if (keyFiles.size() < currentFiles.size()) {
            prompt.append("... 还有 ").append(currentFiles.size() - keyFiles.size())
                    .append(" 个文件未显示 ...\n\n");
        }

        prompt.append("### 修复要求\n");
        prompt.append("1. 仔细分析上述错误信息，定位问题根本原因\n");
        prompt.append("2. 修复所有导致错误的代码、配置或依赖问题\n");
        prompt.append("3. 确保修复后的代码能够正常编译、构建和运行\n");
        prompt.append("4. 必须输出所有文件的完整内容，包括未修改的文件\n");
        prompt.append("5. 严格按照系统提示的输出格式返回修复后的代码\n");
        if (hasImage) {
            prompt.append("6. 若附带参考图，修复后的界面应继续贴近参考图的布局和配色\n");
        }

        return prompt.toString();
    }

    // 只包含关键文件内容，避免提示词过长
    private Map<String, String> filterKeyFiles(Map<String, String> allFiles, String appType) {
        // 优先级: 配置文件 > 入口文件 > 其它文件
        return allFiles.entrySet().stream()
                .filter(entry -> isKeyFile(entry.getKey(), appType))
                .limit(15) // 最多包含 15 个关键文件
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }
    /**
     * 判断是否为关键文件
     */
    private boolean isKeyFile(String path, String appType) {
        String lower = path.toLowerCase(Locale.ROOT);

        // 配置文件始终包含
        if (lower.contains("package.json") || lower.contains("pom.xml") ||
                lower.contains("vite.config") || lower.contains("application.properties") ||
                lower.contains("application.yml")) {
            return true;
        }

        // 入口文件
        if (lower.contains("main.") || lower.contains("app.") ||
                lower.contains("index.html") || lower.contains("application.java")) {
            return true;
        }

        // 根据应用类型选择
        String type = appType == null ? "" : appType;
        if (AppTypesEnum.Html.name().equalsIgnoreCase(type)) {
            return lower.endsWith(".html");
        }
        if (AppTypesEnum.Vue3.name().equalsIgnoreCase(type)) {
            return lower.endsWith(".vue") || lower.endsWith(".js")
                    || lower.endsWith(".ts") || lower.contains("router");
        }
        if (AppTypesEnum.Spring_Vue3.name().equalsIgnoreCase(type)) {
            return lower.endsWith(".vue") || lower.endsWith(".java")
                    || lower.contains("controller") || lower.contains("service");
        }
        return true;
    }
    /**
     * 系统提示词
     */
    private String fixSystemPrompt() {
        return String.join("\n",
                "你是一个专业的代码调试和修复专家，精通全栈开发和错误诊断。",
                "你的任务是分析应用生成或构建过程中出现的错误，并提供完整的修复方案。",
                "",
                "### 错误分析能力",
                "- **编译错误**: 语法错误、类型错误、导入缺失、API 使用错误",
                "- **构建错误**: 依赖缺失、版本冲突、配置错误、打包失败",
                "- **运行时错误**: 启动异常、端口冲突、资源加载失败、配置错误",
                "- **依赖问题**: package.json、pom.xml 中的依赖配置错误",
                "- **配置问题**: vite.config.js、application.properties 等配置文件错误",
                "",
                "### 修复策略",
                "1. **精确定位**: 根据错误信息准确定位问题文件和代码行",
                "2. **最小修改**: 只修改必要的部分，保持其他代码不变",
                "3. **完整输出**: 必须输出所有文件，即使某些文件没有修改",
                "4. **保证质量**: 修复后的代码必须能够编译通过、构建成功、正常运行",
                "",
                "### 常见问题修复模式",
                "#### HTML 应用",
                "- 修复 JavaScript 语法错误",
                "- 修正 DOM 操作错误",
                "- 修复资源引用路径",
                "",
                "#### Vue3 应用",
                "- 修复 package.json 中的依赖版本（vue 固定 3.3.11，禁止 ^ 升到 3.5）",
                "- 修正 vite.config.js 配置（使用 path 时必须先 import/require，禁止 ReferenceError: path is not defined）",
                "- 修复组件语法错误",
                "- 修正路由配置错误",
                "- 添加缺失的依赖",
                "",
                "#### Vue3_Spring 应用",
                "- 前端: 同 Vue3 应用的修复策略",
                "- 后端: 修复 pom.xml 依赖配置",
                "- 后端: 修正 Java 语法错误、注解错误",
                "- 后端: 修复 Spring Boot 配置",
                "- 后端: 确保 Controller 路径正确 (/api 前缀)",
                "",
                "### 输出格式要求 (关键)",
                "你必须严格按照以下格式输出修复后的完整代码：",
                "1. **第一行**: 必须且仅输出 `APP_TYPE=<HTML|VUE3|VUE3_SPRING>`",
                "2. **文件内容**: 紧接着按以下格式输出每个文件：",
                "FILE: <relative_path>",
                "```<language>",
                "<complete_file_content>",
                "```",
                "   - `<relative_path>`: 文件的相对路径（如 `index.html`, `frontend/src/App.vue`, `backend/src/main/resources/application.properties`）。",
                "   - `<complete_file_content>`: **完整**的文件内容，**绝对禁止**省略、使用占位符或 `// ...`。",
                "",
                "### 重要约束",
                "- 必须输出所有文件的完整内容，包括未修改的文件",
                "- 禁止使用占位符、省略号或 `// ...` 来省略代码",
                "- 禁止添加任何解释性文字、注释或修复说明",
                "- 只输出 APP_TYPE 和文件内容，不输出其他任何内容"
        );
    }
}
