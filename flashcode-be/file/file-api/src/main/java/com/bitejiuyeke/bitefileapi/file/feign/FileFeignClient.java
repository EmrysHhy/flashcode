package com.bitejiuyeke.bitefileapi.file.feign;

import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitefileapi.file.domain.vo.FileVO;
import com.bitejiuyeke.bitefileapi.file.domain.vo.SignVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务远程调用
 */
@FeignClient(contextId = "fileFeignClient", value = "bite-file")
public interface FileFeignClient {

    /**
     * 上传应用截图
     *
     * @param file 截图文件
     * @param appId 应用 ID
     * @return 文件 URL 与路径
     */
    @PostMapping(value = "/upload/screenshot", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    R<FileVO> uploadAppScreenShot(@RequestPart("file") MultipartFile file, @RequestParam("appId") Long appId);

    /**
     * 上传应用配图。对象键为 pathPrefix + app/{appId}/{uuid}.ext，公开读。
     *
     * @param file 图片文件
     * @param appId 应用 ID
     * @return 文件 URL 与路径
     */
    @PostMapping(value = "/upload/stock", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    R<FileVO> uploadStockImage(@RequestPart("file") MultipartFile file, @RequestParam("appId") Long appId);

    /**
     * 上传用户头像
     *
     * @param file 头像文件
     * @param userId 用户 ID
     * @return 文件 URL 与路径
     */
    @PostMapping(value = "/upload/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    R<FileVO> uploadUserAvatar(@RequestPart("file") MultipartFile file, @RequestParam("userId") Long userId);

    /**
     * 获取 OSS 直传签名
     *
     * @return 签名信息
     */
    @GetMapping("/sign")
    R<SignVO> getSign();
}
