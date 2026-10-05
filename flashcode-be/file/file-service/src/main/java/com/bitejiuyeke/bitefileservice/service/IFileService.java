package com.bitejiuyeke.bitefileservice.service;

import com.bitejiuyeke.bitefileapi.file.domain.vo.FileVO;
import com.bitejiuyeke.bitefileapi.file.domain.vo.SignVO;
import org.springframework.web.multipart.MultipartFile;

public interface IFileService {

    /**
     * 上传应用截图
     *
     * @param file 截图文件
     * @param appId 应用 ID
     * @return 文件地址
     */
    FileVO uploadAppScreenShot(MultipartFile file, Long appId);

    /**
     * 上传用户头像
     *
     * @param file 头像文件
     * @param userId 用户 ID
     * @return 文件地址
     */
    FileVO uploadUserAvatar(MultipartFile file, Long userId);

    SignVO getSign();
}
