package com.bitejiuyeke.biteportalservice.flash.agent.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.bitejiuyeke.biteportalservice.flash.constants.FlashcodeConstant;
import com.bitejiuyeke.biteportalservice.flash.service.IGiteeService;
import com.bitejiuyeke.biteportalservice.flash.utils.FileWriterUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Emrys
 * content:
 */
@Slf4j
public class CommitNode implements NodeAction {
    private IGiteeService giteeService;
    private Integer deleteCodeExpire;
    private ScheduledExecutorService scheduledExecutorService;

    public CommitNode(IGiteeService giteeService,
                      Integer deleteCodeExpire,
                      ScheduledExecutorService scheduledExecutorService) {
        this.giteeService = giteeService;
        this.deleteCodeExpire = deleteCodeExpire;
        this.scheduledExecutorService = scheduledExecutorService;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("\n开始推送代码并删除本地文件\n");
        int commitAttempt = state.value(FlashcodeConstant.COMMIT_ATTEMPT, Integer.class).orElse(0) + 1;
        try{
            String photoPathStr = state.value(FlashcodeConstant.PHOTO_PATH, String.class).orElse(null);
            Path photoPath = photoPathStr != null ? Path.of(photoPathStr) : null;
            Long appId = state.value(FlashcodeConstant.APP_ID, Long.class).orElse(null);
            Map<String, String> files = FileWriterUtil.readSourceFiles(
                    state.value(FlashcodeConstant.CODE_PATH, String.class).orElse(null));
            // 源码推到 Gitee flash-user-code/{appId}/，后续删本地后可再 pull
            giteeService.push(appId, files);
            //定时删除
            scheduledExecutorService.schedule(() -> {
                try {
                    FileWriterUtil.deleteCodeByAppId(appId);
                    FileWriterUtil.deleteReferenceByAppId(appId);
                    if (photoPath != null) {
                        Files.deleteIfExists(photoPath);
                    }
                } catch (Exception e) {
                    log.error("延迟删除本地文件失败, appId={}", appId, e);
                }
            }, deleteCodeExpire, TimeUnit.HOURS);
            Map<String, Object> result = new HashMap<>();
            result.put(FlashcodeConstant.APP_IS_COMMIT, Boolean.TRUE);
            result.put(FlashcodeConstant.COMMIT_ATTEMPT, commitAttempt);
            return result;
        }catch (Exception e){
            log.error("CommitNode apply error: ", e);
            Map<String, Object> result = new HashMap<>();
            result.put(FlashcodeConstant.APP_IS_COMMIT, Boolean.FALSE);
            result.put(FlashcodeConstant.COMMIT_ERROR_MESSAGE, e.getMessage());
            result.put(FlashcodeConstant.COMMIT_ATTEMPT, commitAttempt);
            return result;
        }
    }
}
