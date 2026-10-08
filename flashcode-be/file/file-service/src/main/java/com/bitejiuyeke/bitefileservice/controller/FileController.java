package com.bitejiuyeke.bitefileservice.controller;

import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitefileapi.file.domain.vo.FileVO;
import com.bitejiuyeke.bitefileapi.file.domain.vo.SignVO;
import com.bitejiuyeke.bitefileapi.file.feign.FileFeignClient;
import com.bitejiuyeke.bitefileservice.service.IFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
public class FileController implements FileFeignClient {

    @Autowired
    private IFileService fileService;

    @Override
    public R<FileVO> uploadAppScreenShot(MultipartFile file, Long appId) {
        return R.ok(fileService.uploadAppScreenShot(file, appId));
    }

    @Override
    public R<FileVO> uploadStockImage(MultipartFile file, Long appId) {
        return R.ok(fileService.uploadStockImage(file, appId));
    }

    @Override
    public R<FileVO> uploadUserAvatar(MultipartFile file, Long userId) {
        return R.ok(fileService.uploadUserAvatar(file, userId));
    }

    @Override
    public R<SignVO> getSign() {
        return R.ok(fileService.getSign());
    }
}
