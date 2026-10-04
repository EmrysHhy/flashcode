package com.bitejiuyeke.bitefileapi.file.feign;

import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitefileapi.file.domain.vo.FileVO;
import com.bitejiuyeke.bitefileapi.file.domain.vo.SignVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务远程调用
 */
@FeignClient(contextId = "fileFeignClient", value = "bite-file")
public interface FileFeignClient {

    /**
     * 上传文件到 OSS
     *
     * @param file 文件
     * @return 文件 URL 与路径
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    R<FileVO> upload(@RequestPart("file") MultipartFile file);

    /**
     * 获取 OSS 直传签名
     *
     * @return 签名信息
     */
    @GetMapping("/sign")
    R<SignVO> getSign();
}
