package com.bitejiuyeke.portalservice.flash.agent.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.bitecommondomain.exception.BuildStageException;
import com.bitejiuyeke.portalservice.flash.constants.FlashcodeConstant;
import com.bitejiuyeke.portalservice.flash.enums.AppTypesEnum;
import com.bitejiuyeke.portalservice.flash.mapper.AppMapper;
import com.bitejiuyeke.portalservice.flash.utils.CommandUtil;
import com.bitejiuyeke.portalservice.flash.utils.FileWriterUtil;
import com.github.dockerjava.api.DockerClient;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;


/**
 *
 * @author Emrys
 * content:
 */
@Slf4j
public class BuildPreviewNode implements NodeAction {

    private static final String DEFAULT_NGINX_PRE = "http://192.168.56.107:80/preview/";
    private static final String DEFAULT_CONTAINER_NAME = "flashcode-userapp-preview";

    private final DockerClient dockerClient;
    private final AppMapper appMapper;
    private final String nginxPre;

    public BuildPreviewNode(DockerClient dockerClient, AppMapper appMapper, String nginxPre) {
        this.dockerClient = dockerClient;
        this.appMapper = appMapper;
        String prefix = nginxPre == null || nginxPre.isBlank() ? DEFAULT_NGINX_PRE : nginxPre;
        this.nginxPre = prefix.endsWith("/") ? prefix : prefix + "/";
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("\n开始构建预览，appId: {}\n", state.value(FlashcodeConstant.APP_ID, Long.class).orElse(null));
        if (!state.value(FlashcodeConstant.APP_IS_GENERATE, Boolean.class).orElse(false)) {
            log.error("应用代码未生成，无法构建预览，appId: {}", state.value(FlashcodeConstant.APP_ID, Long.class).orElse(null));
            throw new ServiceException("应用代码未生成，无法构建预览,请重试或者联系管理员");
        }
        try {
            String appTypeName = state.value(FlashcodeConstant.APP_TYPE, String.class).orElse(null);
            AppTypesEnum appType = AppTypesEnum.of(appTypeName);
            String codePathStr = state.value(FlashcodeConstant.CODE_PATH, String.class).orElse(null);
            Path codePath = codePathStr != null ? Path.of(codePathStr) : null;
            Long appId = state.value(FlashcodeConstant.APP_ID, Long.class).orElse(null);
            packageCode(appType, codePath, appId);
            String url = nginxPre + appId + "/#/";
            int updated = appMapper.updateUrlById(appId, url);
            if (updated <= 0) {
                throw new BuildStageException(FlashcodeConstant.STAGE_UPDATE_URL, false,
                        "更新预览地址失败, appId=" + appId + ", url=" + url, null);
            }
            return Map.of(FlashcodeConstant.APP_IS_BUILD, Boolean.TRUE,
                    FlashcodeConstant.PREVIEW_URL, url);
        } catch (BuildStageException e) {
            log.error("构建预览失败，appId={}, stage={}", state.value(FlashcodeConstant.APP_ID, Long.class).orElse(null),
                    e.getStage(), e);
            return fail(e.getStage(), e.isFixable(), e.getMessage());
        } catch (Exception e) {
            log.error("构建预览失败，appId: {}, error: {}", state.value(FlashcodeConstant.APP_ID, Long.class).orElse(null), e.getMessage(), e);
            return fail(FlashcodeConstant.STAGE_UNKNOWN, true, e.getMessage());
        }
    }

    private static Map<String, Object> fail(String stage, boolean fixable, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put(FlashcodeConstant.APP_IS_BUILD, Boolean.FALSE);
        result.put(FlashcodeConstant.ERROR_TYPE, stage);
        result.put(FlashcodeConstant.ERROR_FIXABLE, fixable);
        result.put(FlashcodeConstant.BUILD_ERROR_MESSAGE, message);
        return result;
    }

    /**
     * 打包代码并且保存到预览目录。每一步标阶段，失败时保留命令原文。
     */
    private void packageCode(AppTypesEnum appType, Path loadedCode, Long appId) {
        switch (appType) {
            case Html -> runStage(FlashcodeConstant.STAGE_COPY_PREVIEW, true,
                    () -> FileWriterUtil.copyHtmlToPreview(loadedCode, appId));
            case Vue3 -> {
                runStage(FlashcodeConstant.STAGE_NPM_INSTALL, true,
                        () -> CommandUtil.runCommand(FlashcodeConstant.CMD_NPM_INSTALL, loadedCode));
                runStage(FlashcodeConstant.STAGE_NPM_BUILD, true,
                        () -> CommandUtil.runCommand(FlashcodeConstant.CMD_NPM_BUILD, loadedCode));
                runStage(FlashcodeConstant.STAGE_COPY_PREVIEW, true,
                        () -> FileWriterUtil.copyDistPreview(loadedCode, appId));
            }
            case Spring_Vue3 -> {
                Path frontDir = loadedCode.resolve("frontend");
                runStage(FlashcodeConstant.STAGE_NPM_INSTALL, true,
                        () -> CommandUtil.runCommand(FlashcodeConstant.CMD_NPM_INSTALL, frontDir));
                runStage(FlashcodeConstant.STAGE_NPM_BUILD, true,
                        () -> CommandUtil.runCommand(FlashcodeConstant.CMD_NPM_BUILD, frontDir));
                runStage(FlashcodeConstant.STAGE_COPY_PREVIEW, true,
                        () -> FileWriterUtil.copyDistPreview(frontDir, appId));
                Path backDir = loadedCode.resolve("backend");
                runStage(FlashcodeConstant.STAGE_MVN_PACKAGE, true,
                        () -> CommandUtil.runCommand(FlashcodeConstant.CMD_MVN_PACKAGE, backDir));
                Path workDir = runStage(FlashcodeConstant.STAGE_COPY_PREVIEW, true,
                        () -> FileWriterUtil.copyJarPreview(backDir, appId));
                runStage(FlashcodeConstant.STAGE_START_JAR, false,
                        () -> CommandUtil.runJar(dockerClient, workDir, appId, DEFAULT_CONTAINER_NAME));
            }
        }
    }

    private static void runStage(String stage, boolean fixable, Runnable action) {
        try {
            action.run();
        } catch (BuildStageException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildStageException(stage, fixable, e.getMessage(), e);
        }
    }

    private static <T> T runStage(String stage, boolean fixable, java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (BuildStageException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildStageException(stage, fixable, e.getMessage(), e);
        }
    }
}
