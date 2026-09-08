package com.bitejiuyeke.bitefileservice.service;

import com.bitejiuyeke.bitefileservice.domain.dto.FileDTO;
import com.bitejiuyeke.bitefileservice.domain.dto.SignDTO;
import org.springframework.web.multipart.MultipartFile;

public interface IFileService {
    FileDTO upload(MultipartFile file);

    SignDTO getSign();
}
