package com.bitejiuyeke.portalservice.flash.service.implement;

import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.bitecommonsecurity.domain.dto.LoginUserDTO;
import com.bitejiuyeke.bitecommonsecurity.service.TokenService;
import com.bitejiuyeke.portalservice.flash.constants.FlashcodeConstant;
import com.bitejiuyeke.portalservice.flash.domain.dto.require.AppEditParam;
import com.bitejiuyeke.portalservice.flash.domain.dto.result.AppDetailDTO;
import com.bitejiuyeke.portalservice.flash.domain.dto.result.GenerateAppDTO;
import com.bitejiuyeke.portalservice.flash.domain.dto.result.StockImageBatch;
import com.bitejiuyeke.portalservice.flash.domain.entity.AppDO;
import com.bitejiuyeke.portalservice.flash.domain.entity.ChatHistoryDO;
import com.bitejiuyeke.portalservice.flash.enums.AppTypesEnum;
import com.bitejiuyeke.portalservice.flash.enums.DeployStatusEnum;
import com.bitejiuyeke.portalservice.flash.enums.Role;
import com.bitejiuyeke.portalservice.flash.mapper.AppMapper;
import com.bitejiuyeke.portalservice.flash.mapper.ChatHistoryMapper;
import com.bitejiuyeke.portalservice.flash.service.IAppService;
import com.bitejiuyeke.portalservice.flash.service.IGiteeService;
import com.bitejiuyeke.portalservice.flash.utils.AnalysisUtil;
import com.bitejiuyeke.portalservice.flash.utils.ChatContentSupport;
import com.bitejiuyeke.portalservice.flash.utils.CommandUtil;
import com.bitejiuyeke.portalservice.flash.utils.FileWriterUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

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
    ChatHistoryMapper chatHistoryMapper;
    @Autowired
    TokenService tokenService;
    @Autowired
    IGiteeService giteeService;
    @Autowired
    DockerClient dockerClient;
    @Autowired
    VectorStore vectorStore;
    @Autowired
    ImageSearchService imageSearchService;
    @Autowired
    Executor threadPoolTaskExecutor;
    @Value("${flashcode.delete-code-expire:12}")
    private Integer deleteCodeExpire;
    @Value("${vscode.host:192.168.56.107}")
    private String vscodeHost;
    @Value("${vscode.port:8080}")
    private String vscodeport;
    @Value("${flashcode.preview.nginx_pre:http://192.168.56.107:80/preview/}")
    private String NGINX_PRE;
    @Value("${flashcode.preview.container_name:flashcode-userapp-preview}")
    private String PREVIEW_CONTAINER_NAME;
    @Value("${flashcode.deploy.image:flashcode/user-deploy}")
    private String deployImage;
    @Value("${flashcode.deploy.host:http://192.168.56.107}")
    private String deployHost;
    @Value("${flashcode.deploy.maxRetries:10}")
    private Integer deployMaxRetries;
    @Value("${flashcode.deploy.containerName:flashcode-userapp}")
    private String deployContainerName;
    @Value("${flashcode.deploy.nginxPath:/root/emrys-java/flashcode/deploy/dev/app/require-image/deploy/nginx/config/nginx.conf}")
    private String deployNginxPath;
    @Value("${flashcode.deploy.deployPath:/root/emrys-java/flashcode/deploy/dev/data/flashcodedata/flashcode-app/user-deploy}")
    private String deployPath;
    @Value("${flashcode.deploy.local-dir:/workspace/user-deploy}")
    private String deployLocalDir;

    /**
     * app应用生成
     *
     * @param appId
     * @param requirement
     * @return
     */
   // @Override
    /*public GenerateAppDTO appGenerate(Long appId, String requirement) {
        LoginUserDTO loginUser = tokenService.getLoginUser();
        Long ownerId = loginUser == null ? null : loginUser.getUserId();
        appMapper.insertIfAbsent(appId, ownerId);
        if (ownerId != null) {
            appMapper.bindOwnerIfPlaceholder(appId, ownerId);
        }
        //生成代码
        String appCode = ChatContentSupport.collect(chatClient.prompt()
                .system(getSysPrompt(appId))
                .user(getUserPrompt(requirement))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(appId))
                        .param(FlashcodeConstant.USER_ID, ownerId)
                        .param(FlashcodeConstant.APP_ID, appId))
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
        String url = NGINX_PRE + appId + "/#/"; //  /workspace/user-preview
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
    }*/

    /**
     * 获取应用详情
     * @param appId
     * @return
     */
    @Override
    public AppDetailDTO getAppDetail(Long appId) {
        AppDO app = appMapper.selectById(appId);
        if (app == null) {
            throw new ServiceException("应用不存在");
        }
        if (!Integer.valueOf(DeployStatusEnum.DEPLOYED.getValue()).equals(app.getDeployStatus())) {
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
        appDetailDTO.setAppDoc(app.getAppDoc());
        return appDetailDTO;
    }

    /**
     * 可视化编辑：只改用户点中的那一个 DOM 元素。
     * 规则：按修改说明搜配图后写入提示词；不把整份源码写入聊天记忆；
     * 模型必须仍按 FILE: 完整文件输出，解析后合并进原 codes 再写盘、预览。
     */
    @Override
    public GenerateAppDTO appEdit(AppEditParam appEditParam) {
        Long appId = appEditParam.getAppId();
        AppDO app = appMapper.selectById(appId);
        if (app == null) {
            throw new ServiceException("应用不存在");
        }
        Path appDir = Paths.get(FlashcodeConstant.USER_CODE_DIR, String.valueOf(appId))
                .toAbsolutePath()
                .normalize();
        if (!Files.isDirectory(appDir)) {
            log.info("本地源码不存在，从 Gitee 拉取, appId={}, dir={}", appId, appDir);
            giteeService.pull(appId);
        }
        Map<String,String> codes = FileWriterUtil.readSourceFiles(appDir);
        String searchText = app.getAppDoc() == null ? "" : app.getAppDoc();
        if (appEditParam.getNewContent() != null && !appEditParam.getNewContent().isBlank()) {
            searchText = searchText + "\n" + appEditParam.getNewContent();
        }
        StockImageBatch stockImages = imageSearchService.searchForRequirement(appId, searchText);
        List<String> keptUrls = new ArrayList<>();
        String editAppUserPrompt = getEditAppUserPrompt(codes, appEditParam.getElementSelector(),
                appEditParam.getNewContent(), stockImages, keptUrls);
        String editAppSysPrompt = getEditAppSysPrompt(appEditParam.getElementSelector(),
                appEditParam.getNewContent(), stockImages != null && !stockImages.isEmpty());
        //获得大模型修改之后的代码
        // 不写入会话记忆。用户提示词里带了全部源码，chat_history.content 装不下，下一轮也会把源码再喂给模型
        LoginUserDTO loginUser = tokenService.getLoginUser();
        Long userId = loginUser == null ? null : loginUser.getUserId();
        String appCode = ImageSearchService.unmaskUrls(stockImages.restore(
                ChatContentSupport.collect(chatClient.prompt()
                .system(editAppSysPrompt)
                .user(editAppUserPrompt)
                .advisors(a -> a.param(FlashcodeConstant.USER_ID, userId)
                        .param(FlashcodeConstant.APP_ID, appId))
                .advisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder().build())
                        .build()))), keptUrls);
        log.info("修改应用代码完成，appId: {}", appId);

        Map<String, String> editedFiles = AnalysisUtil.getFiles(appCode);
        if (editedFiles.isEmpty()) {
            throw new ServiceException("未解析到修改后的代码");
        }
        codes.putAll(editedFiles);
        AppTypesEnum appType = AnalysisUtil.resolveType(appCode, codes);
        //更新数据库类型 updateType(appId, appType)
        int updated = appMapper.updateTypeById(appId, appType.getValue());
        if (updated <= 0) {
            throw new ServiceException("更新应用类型失败，app 不存在, appId=" + appId + ", appType=" + appType.getValue());
        }
        //本地代码保存。模型只回修改过的文件，其余源码仍留在 codes 里
        Path codePath = FileWriterUtil.saveCode(appId, codes);
        // 源码推到 Gitee flash-user-code/{appId}/，后续删本地后可再 pull
        giteeService.push(appId, codes);
        // 1.根据类型编译打包    //VUE3进入 build->dist   //VUE3+Spring -> jar + dist
        // 2.html,dist,jar包保存到 /workspace/user-preview 会映射到宿主机 /deploy/dev/data/flashcodedata/flashcode-app/user-preview
        packageCode(appType, codePath, appId);
        // 3. 得到URL预览地址
        String url = NGINX_PRE + appId + "/#/"; //  /workspace/user-preview
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
        saveEditChat(appId, appEditParam.getElementSelector(), appEditParam.getNewContent());

        //返回DTO
        GenerateAppDTO generateAppDTO = new GenerateAppDTO();
        generateAppDTO.setAppId(appId);
        generateAppDTO.setAppType(appType);
        generateAppDTO.setUrl(url);
        log.info("代码编辑成功");
        return generateAppDTO;
    }
    /**
     * 获取应用源代码
     * @param appId
     * @return
     */
    @Override
    public String getSrc(Long appId) {
        Path appDir = Paths.get(FlashcodeConstant.USER_CODE_DIR, String.valueOf(appId))
                .toAbsolutePath()
                .normalize();
        if (!Files.isDirectory(appDir)) {
            log.info("本地源码不存在，从 Gitee 拉取, appId={}, dir={}", appId, appDir);
            giteeService.pull(appId);
        }
        return "http://" + vscodeHost + ":" + vscodeport + "/?folder=/workspace/user-code/" + appId;
    }
    /**
     * 高级编辑功能
     * @param appId
     * @return
     */
    @Override
    public GenerateAppDTO appAdvancedEdit(Long appId) {
        // 再次检查一遍,防止用户太久没有上线,本地代码已经删除了
        Path appDir = Paths.get(FlashcodeConstant.USER_CODE_DIR, String.valueOf(appId))
                .toAbsolutePath()
                .normalize();
        if (!Files.isDirectory(appDir)) {
            log.info("本地源码不存在，从 Gitee 拉取, appId={}, dir={}", appId, appDir);
            giteeService.pull(appId);
        }
        Map<String,String> codes = FileWriterUtil.readSourceFiles(appDir);
        AppTypesEnum appType = AppTypesEnum.of(String.valueOf(appMapper.selectTpyeById(appId)));
        //本地代码保存。模型只回修改过的文件，其余源码仍留在 codes 里
        Path codePath = FileWriterUtil.saveCode(appId, codes);
        // 源码推到 Gitee flash-user-code/{appId}/，后续删本地后可再 pull
        giteeService.push(appId, codes);
        // 1.根据类型编译打包    //VUE3进入 build->dist   //VUE3+Spring -> jar + dist
        // 2.html,dist,jar包保存到 /workspace/user-preview 会映射到宿主机 /deploy/dev/data/flashcodedata/flashcode-app/user-preview
        packageCode(appType, codePath, appId);
        // 3. 得到URL预览地址
        String url = NGINX_PRE + appId + "/#/"; //  /workspace/user-preview
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
        log.info("代码编辑成功");
        return generateAppDTO;
    }

    /**
     * 应用公开部署
     * @param appId
     * @return
     */
    @Override
    public String appDeploy(Long appId) {
        AppDO app = appMapper.selectById(appId);
        if (app == null) {
            throw new ServiceException("应用不存在");
        }
        if (Integer.valueOf(DeployStatusEnum.DEPLOYED.getValue()).equals(app.getDeployStatus())) {
            log.info("应用已经部署完毕,直接返回URL");
            FileWriterUtil.copyPreviewDistToDeploy(appId, deployAppDir(appId));
            startDeployBackend(appId);
            return app.getAppUrl();
        }
        Path appDir = Paths.get(FlashcodeConstant.USER_CODE_DIR, String.valueOf(appId))
                .toAbsolutePath()
                .normalize();
        if (!Files.isDirectory(appDir)) {
            log.info("本地源码不存在，从 Gitee 拉取, appId={}, dir={}", appId, appDir);
            giteeService.pull(appId);
        }
        //编译构建
        Map<String,String> codes = FileWriterUtil.readSourceFiles(appDir);
        AppTypesEnum appType = AppTypesEnum.of(String.valueOf(appMapper.selectTpyeById(appId)));
        //本地代码保存。模型只回修改过的文件，其余源码仍留在 codes 里
        Path codePath = FileWriterUtil.saveCode(appId, codes);
        // 源码推到 Gitee flash-user-code/{appId}/，后续删本地后可再 pull
        giteeService.push(appId, codes);

        // 创建一个单独的容器
        int hostPort = createContainer(appId);
        String url = deployAccessUrl(appId, hostPort);
        appMapper.updateDeployById(appId, url);
        log.info("发布容器已创建, appId={}, url={}", appId, url);
        // 1.根据类型编译打包    //VUE3进入 build->dist   //VUE3+Spring -> jar + dist
        // 2.html,dist,jar包保存到 /workspace/user-preview 会映射到宿主机 /deploy/dev/data/flashcodedata/flashcode-app/user-preview
        packageCode(appType, codePath, appId);
        FileWriterUtil.copyPreviewDistToDeploy(appId, deployAppDir(appId));
        startDeployBackend(appId);
        log.info("发布成功,请访问url:{}",url);
        return url;
    }

    /**
     * Spring 应用的列表和图片在后端接口里。发布容器只挂了前端 dist 时，页面标题还在，列表是空的。
     */
    private void startDeployBackend(Long appId) {
        java.util.Optional<Path> jar = FileWriterUtil.copyPreviewJarToDeploy(appId, deployAppDir(appId));
        if (jar.isEmpty()) {
            log.info("没有后端 jar，跳过发布进程, appId={}", appId);
            return;
        }
        String containerName = deployContainerName + "-" + appId;
        CommandUtil.runDeployJar(dockerClient, containerName, appId, jar.get().getFileName().toString());
    }

    /**
     * 创建一个容器,容器名称与appId相联系,方便后续问题排查。
     * 宿主机端口冲突时在 8001-9999 内顺延。
     *
     * @return 实际映射到宿主机的端口
     */
    private int createContainer(Long appId) {
        String containerName = deployContainerName + "-" + appId;
        prepareDeployDir(appId);
        removeContainerIfExists(containerName);

        int port = CommandUtil.generatePort(appId);
        int maxRetries = deployMaxRetries == null || deployMaxRetries < 1 ? 1 : deployMaxRetries;
        DockerException lastConflict = null;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                String containerId = startDeployContainer(containerName, appId, port);
                log.info("发布容器已启动, name={}, hostPort={}, id={}", containerName, port, containerId);
                return port;
            } catch (DockerException e) {
                if (!isPortConflict(e) || attempt == maxRetries - 1) {
                    throw new ServiceException("创建发布容器失败: " + e.getMessage());
                }
                lastConflict = e;
                log.warn("宿主机端口 {} 已被占用, 改用下一个端口, appId={}", port, appId);
                port = nextDeployPort(port);
            }
        }
        throw new ServiceException("创建发布容器失败: " + (lastConflict == null ? "端口均被占用" : lastConflict.getMessage()));
    }

    /**
     * 门户容器里的 /workspace/user-deploy 就是宿主机 user-deploy。
     */
    private Path deployAppDir(Long appId) {
        Path localParent = Path.of(deployLocalDir);
        if (Files.isDirectory(localParent)) {
            return localParent.resolve(String.valueOf(appId));
        }
        return Path.of(deployPath, String.valueOf(appId));
    }

    /**
     * 创建该应用的发布目录。门户跑在容器里时写挂载目录，否则写宿主机目录。
     */
    private void prepareDeployDir(Long appId) {
        Path target = deployAppDir(appId);
        try {
            Files.createDirectories(target);
        } catch (IOException e) {
            throw new ServiceException("创建发布目录失败: " + target);
        }
    }

    /**
     * 创建并启动发布容器。宿主机端口映射到容器内 80。
     *
     * @return 容器 ID
     */
    private String startDeployContainer(String containerName, Long appId, int hostPort) {
        String appDirName = String.valueOf(appId);
        ExposedPort containerPort = ExposedPort.tcp(80);
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withPortBindings(PortBinding.parse(hostPort + ":80"))
                .withBinds(
                        Bind.parse(deployPath + "/" + appDirName + ":/workspace/user-deploy/" + appDirName),
                        Bind.parse(deployScriptDir() + ":/workspace/scripts:ro"),
                        Bind.parse(deployNginxPath + ":/etc/nginx/nginx.conf:ro")
                );
        CreateContainerResponse created = dockerClient.createContainerCmd(deployImage)
                .withName(containerName)
                .withExposedPorts(containerPort)
                .withHostConfig(hostConfig)
                .exec();
        try {
            dockerClient.startContainerCmd(created.getId()).exec();
            return created.getId();
        } catch (DockerException e) {
            removeContainerQuietly(created.getId());
            throw e;
        }
    }

    /**
     * 同名容器已存在时先删掉，避免名称冲突被当成端口冲突。
     */
    private void removeContainerIfExists(String containerName) {
        try {
            dockerClient.inspectContainerCmd(containerName).exec();
            dockerClient.removeContainerCmd(containerName).withForce(true).exec();
            log.info("已删除同名发布容器: {}", containerName);
        } catch (NotFoundException ignored) {
            // 首次发布没有同名容器
        }
    }

    /**
     * 启动失败时删掉已创建但未运行的容器。
     */
    private void removeContainerQuietly(String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        } catch (DockerException e) {
            log.warn("删除未启动的发布容器失败, id={}, error={}", containerId, e.getMessage());
        }
    }

    /**
     * 判断 Docker 报错是不是宿主机端口已被占用。
     */
    private static boolean isPortConflict(DockerException e) {
        String message = e.getMessage();
        if (message == null && e.getCause() != null) {
            message = e.getCause().getMessage();
        }
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("port is already allocated") || lower.contains("address already in use");
    }

    /**
     * 根据 nginx 配置文件位置找到旁边的 script 目录。
     */
    private Path deployScriptDir() {
        Path nginxFile = Path.of(deployNginxPath);
        Path parent = nginxFile.getParent();
        if (parent == null) {
            throw new ServiceException("发布 nginx 配置路径不正确: " + deployNginxPath);
        }
        Path besideConf = parent.resolve("script");
        if (Files.isDirectory(besideConf)) {
            return besideConf;
        }
        if (parent.getParent() != null) {
            Path besideNginx = parent.getParent().resolve("script");
            if (Files.isDirectory(besideNginx)) {
                return besideNginx;
            }
        }
        return besideConf;
    }

    /**
     * 拼发布后的访问地址：{host}:{端口}/deploy/{appId}/#/
     */
    private String deployAccessUrl(Long appId, int hostPort) {
        String host = deployHost == null ? "" : deployHost.trim();
        while (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        return host + ":" + hostPort + "/deploy/" + appId + "/#/";
    }

    /**
     * 在 8001-9999 内取下一个端口，到末尾后回到 8001。
     */
    private static int nextDeployPort(int port) {
        int index = Math.floorMod(port - FlashcodeConstant.JAR_HOST_PORT_BASE + 1, FlashcodeConstant.JAR_HOST_PORT_RANGE);
        return FlashcodeConstant.JAR_HOST_PORT_BASE + index;
    }


    private void saveEditChat(Long appId, String elementSelector, String newContent) {
        ChatHistoryDO userMessage = new ChatHistoryDO();
        userMessage.setAppId(appId);
        userMessage.setChatRole(Role.USER.getValue());
        userMessage.setContent("修改元素 " + elementSelector + "：" + newContent);
        chatHistoryMapper.insert(userMessage);

        ChatHistoryDO assistantMessage = new ChatHistoryDO();
        assistantMessage.setAppId(appId);
        assistantMessage.setChatRole(Role.LLM.getValue());
        assistantMessage.setContent("应用修改成功");
        chatHistoryMapper.insert(assistantMessage);
    }

    /**
     * 编辑用户提示词。
     * 规则：写明 CSS 选择器和修改要求；有配图时只列出 IMG_n，换图只能用这些记号；
     * 后面附全部源码，模型只对目标元素做最小修改。
     */
    private String getEditAppUserPrompt(Map<String, String> codes, String elementSelector, String newContent,
                                        StockImageBatch stockImages, List<String> keptUrls) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("【精确修改请求】\n\n");
        prompt.append("目标 CSS 选择器（已精确定位，无需分析）：\n");
        prompt.append(elementSelector).append("\n\n");
        prompt.append("修改类型：按具体要求修改目标元素。\n");
        prompt.append("如果要求涉及图片：只在该元素内部新增或替换一个 img，src 使用主体一致的 IMG_n。\n");
        prompt.append("其他元素上的 KEEP_n 是已有图片地址，必须原样保留。\n");
        prompt.append("具体要求：").append(ImageSearchService.hideUrls(newContent)).append("\n\n");
        if (elementSelector != null && elementSelector.contains(":nth-child(")) {
            prompt.append("⚠ 注意：该选择器包含 nth-child，表示这是列表中的一个单独元素。\n");
            prompt.append("你必须确保其他列表项在任何方面都不发生变化。\n");
        }
        prompt.append(ImageSearchService.imageHint(stockImages == null ? null : stockImages.hints()));
        prompt.append("以下是完整代码文件，请只对目标元素进行最小必要修改：\n\n");
        if (codes != null) {
            for (Map.Entry<String, String> entry : codes.entrySet()) {
                prompt.append("FILE: ").append(entry.getKey()).append("\n");
                prompt.append("```html\n");
                prompt.append(ImageSearchService.maskUrls(entry.getValue(), keptUrls));
                prompt.append("\n```\n\n");
            }
        }
        return prompt.toString();
    }

    /**
     * 编辑系统提示词。
     * 规则：只改选择器命中的单一元素，禁止改兄弟/全局样式；nth-child 只动第 n 项；
     * 换图必须用「可用图片」URL；输出仍是 FILE: 相对路径 + 被改文件的完整内容。
     */
    private String getEditAppSysPrompt(String elementSelector, String newContent, boolean hasStockImages) {
        return String.join("\n",
                "你是一个“精确 DOM 定点修改器”，不是代码重构器。",
                "你的唯一任务：只修改一个已经被精确定位的 DOM 元素，其余任何内容都禁止改动。",
                "",
                "====================",
                "【目标元素（已由系统精确定位）】",
                "CSS 选择器：",
                elementSelector == null ? "" : elementSelector,
                "",
                "该选择器唯一且只指向一个确定的 DOM 元素。",
                "你【不需要】也【不允许】重新理解、简化或泛化该选择器。",
                "",
                "====================",
                "【硬性修改规则（不可违反）】",
                "1. 只允许修改该选择器命中的“单一元素”。",
                "2. 禁止修改兄弟元素和父元素。",
                "3. 禁止改动目标元素以外的 DOM。目标元素内部允许新增或替换一个 img 来放配图。",
                "4. 禁止修改任何全局 CSS、公共 class 或选择器。",
                hasStockImages
                        ? "5. 目标元素需要图片时，用主体一致的 IMG_n 作为 src。色块或文字占位要换成这张 img。其他位置的 KEEP_n 原样保留，禁止编造网址。"
                        : "5. 没有可用图片时不要编造无法访问的图片地址。",
                "",
                "====================",
                "【nth-child 特别规则（最高优先级）】",
                "当前选择器包含 :nth-child(n)，这表示：",
                "- 只允许影响该列表中的第 n 个元素；",
                "- 其他列表项必须在结构、样式、文本上保持 100% 不变。",
                "",
                "当修改涉及颜色、字体、背景、样式时：",
                "✓ 强制优先使用该元素的内联 style；",
                "✓ 或仅对该完整选择器生效的样式；",
                "✗ 绝对禁止修改公共 class（例如 .teacher-name）。",
                "",
                "====================",
                "【修改指令】",
                newContent == null ? "" : newContent,
                "",
                "====================",
                "【输出要求】",
                "- 只输出被修改的文件；",
                "- 文件内容必须是修改后的完整内容；",
                "- 除目标元素外，其他任何字符都不得变化；",
                "- 不要输出解释、注释或多余文本。",
                "- 文件内容：紧接着按以下格式输出每个文件：",
                "FILE: <relative_path>",
                "```<language>",
                "<complete_file_content>",
                "```",
                "  - `<relative_path>`：文件的相对路径，路径中省略appId（如 `index.html`，`frontend/src/App.vue`，`backend/src/main/resources/application.properties`）。",
                "  - `<complete_file_content>`：**完整**的文件内容，**绝对禁止**省略、使用占位符或 `// ...`。"
        );
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
                CommandUtil.runJar(dockerClient,workDir,appId, PREVIEW_CONTAINER_NAME);
            }
        }
    }

}
