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
    public R<FileVO> upload(MultipartFile file) {
        return R.ok(fileService.upload(file));
    }

    @Override
    public R<SignVO> getSign() {
        return R.ok(fileService.getSign());
    }
}
