package com.bitejiuyeke.bitefileservice.service;

import com.bitejiuyeke.bitefileapi.file.domain.vo.FileVO;
import com.bitejiuyeke.bitefileapi.file.domain.vo.SignVO;
import org.springframework.web.multipart.MultipartFile;

public interface IFileService {
    FileVO upload(MultipartFile file);

    SignVO getSign();
}
