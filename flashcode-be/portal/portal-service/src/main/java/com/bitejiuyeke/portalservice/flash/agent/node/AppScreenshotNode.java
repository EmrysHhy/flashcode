package com.bitejiuyeke.portalservice.flash.agent.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommondomain.domain.ResultCode;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.bitefileapi.file.domain.vo.FileVO;
import com.bitejiuyeke.bitefileapi.file.feign.FileFeignClient;
import com.bitejiuyeke.portalservice.flash.constants.FlashcodeConstant;
import com.bitejiuyeke.portalservice.flash.mapper.AppMapper;
import com.bitejiuyeke.portalservice.flash.utils.SeleniumUtil;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.bitejiuyeke.portalservice.flash.constants.FlashcodeConstant.APP_IS_SCREENSHOT;
import static com.bitejiuyeke.portalservice.flash.constants.FlashcodeConstant.SCREENSHOT_ERROR_MESSAGE;

/**
 *
 * @author Emrys
 * content:
 */
public class AppScreenshotNode implements NodeAction {
    private final AppMapper appMapper;
    private final FileFeignClient fileFeignClient;

    public AppScreenshotNode(AppMapper appMapper, FileFeignClient fileFeignClient) {
        this.fileFeignClient = fileFeignClient;
        this.appMapper = appMapper;
    }


    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        int screenshotAttempt = state.value(FlashcodeConstant.SCREENSHOT_ATTEMPT, Integer.class).orElse(0) + 1;
        try {

            Long appId = state.value(FlashcodeConstant.APP_ID, Long.class).orElse(null);
            String url = state.value(FlashcodeConstant.PREVIEW_URL, String.class).orElse(null);
            Path photoPath = SeleniumUtil.screenshot(appId,url);
            if (photoPath == null || !Files.exists(photoPath)) {
                throw new ServiceException("截图失败");
            }
            MultipartFile multipartFile = convertToMultipartFile(photoPath);
            //上传到oss
            R<FileVO> upload = fileFeignClient.uploadAppScreenShot(multipartFile, appId);
            if (upload.getData() == null || upload.getCode() != ResultCode.SUCCESS.getCode()) {
                throw new ServiceException("上传截图失败");
            }
            String onlinePhoto = upload.getData().getUrl();
            //存入数据库
            int updated = appMapper.updateScreenshotById(appId, onlinePhoto);
            if (updated <= 0) {
                throw new ServiceException("更新截图地址失败");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("onlinePhoto", onlinePhoto);
            result.put(APP_IS_SCREENSHOT, Boolean.TRUE);
            result.put(FlashcodeConstant.SCREENSHOT_ATTEMPT, screenshotAttempt);
            result.put(FlashcodeConstant.PHOTO_PATH, photoPath.toString());
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put(APP_IS_SCREENSHOT, Boolean.FALSE);
            result.put(SCREENSHOT_ERROR_MESSAGE, e.getMessage());
            result.put(FlashcodeConstant.SCREENSHOT_ATTEMPT, screenshotAttempt);
            return result;
        }
    }

    /**
     * 将Path转换为MultipartFile
     *
     * @param photo
     * @return
     * @throws IOException
     */
    private MultipartFile convertToMultipartFile(Path photo) throws IOException {
        byte[] content = Files.readAllBytes(photo);
        String filename = photo.getFileName().toString();
        String contentType = Files.probeContentType(photo);
        if (contentType == null) {
            contentType = "image/png";
        }
        return new ByteArrayMultipartFile("file", filename, contentType, content);
    }

    private static class ByteArrayMultipartFile implements MultipartFile {

        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        private ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.write(dest.toPath(), content);
        }
    }
    /**
     *
     */
}
