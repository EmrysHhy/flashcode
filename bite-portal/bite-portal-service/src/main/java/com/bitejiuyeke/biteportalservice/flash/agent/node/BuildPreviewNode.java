package com.bitejiuyeke.biteportalservice.flash.agent.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.biteportalservice.flash.constants.FlashcodeConstant;
import com.bitejiuyeke.biteportalservice.flash.enums.AppTypesEnum;
import com.bitejiuyeke.biteportalservice.flash.mapper.AppMapper;
import com.bitejiuyeke.biteportalservice.flash.utils.CommandUtil;
import com.bitejiuyeke.biteportalservice.flash.utils.FileWriterUtil;
import com.github.dockerjava.api.DockerClient;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Map;

import static com.bitejiuyeke.biteportalservice.flash.constants.FlashcodeConstant.CONTAINER_NAME;

/**
 *
 * @author Emrys
 * content:
 */
@Slf4j
public class BuildPreviewNode implements NodeAction {

    private final DockerClient dockerClient;


    private final AppMapper appMapper;


    public BuildPreviewNode(DockerClient dockerClient,AppMapper appMapper) {
        this.dockerClient = dockerClient;
        this.appMapper = appMapper;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        if(!state.value(FlashcodeConstant.APP_IS_GENERATE, Boolean.class).orElse(false)){
            log.error("应用代码未生成，无法构建预览，appId: {}", state.value(FlashcodeConstant.APP_ID, Long.class).orElse(null));
            throw new ServiceException("应用代码未生成，无法构建预览,请重试或者联系管理员");
        }
        try{
            String appTypeName = state.value(FlashcodeConstant.APP_TYPE, String.class).orElse(null);
            AppTypesEnum appType = AppTypesEnum.of(appTypeName);
            String codePathStr = state.value(FlashcodeConstant.CODE_PATH, String.class).orElse(null);
            Path codePath = codePathStr != null ? Path.of(codePathStr) : null;
            Long appId = state.value(FlashcodeConstant.APP_ID, Long.class).orElse(null);
            // 1.根据类型编译打包    //VUE3进入 build->dist   //VUE3+Spring -> jar + dist
            // 2.html,dist,jar包保存到 /workspace/user-preview 会映射到宿主机 /deploy/dev/data/flashcodedata/flashcode-app/user-preview
            packageCode(appType, codePath, appId);
            // 3. 得到URL预览地址
            String url = FlashcodeConstant.NGINX_PRE + appId + "/#/"; //  /workspace/user-preview
            // 4. 更新数据库中的预览地址
            int updated = appMapper.updateUrlById(appId, url);
            if(updated <= 0){
                log.error("更新预览地址失败，appId: {}, url: {}", appId, url);
                throw new ServiceException("更新预览地址失败");
            }
            //成功
            return Map.of(FlashcodeConstant.APP_IS_BUILD, Boolean.TRUE,
                    FlashcodeConstant.PREVIEW_URL, url);
        }catch (Exception e){
            log.error("构建预览失败，appId: {}, error: {}", state.value(FlashcodeConstant.APP_ID, Long.class).orElse(null), e.getMessage(), e);
            return Map.of(FlashcodeConstant.APP_IS_BUILD, Boolean.FALSE,
                    FlashcodeConstant.ERROR_TYPE, e.getClass().getSimpleName(),
                          FlashcodeConstant.BUILD_ERROR_MESSAGE, e.getMessage());

           // throw new ServiceException("构建预览失败，请重试或者联系管理员");
        }

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
}
