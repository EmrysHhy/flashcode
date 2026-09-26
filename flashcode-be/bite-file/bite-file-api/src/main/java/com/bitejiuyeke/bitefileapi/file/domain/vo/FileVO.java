package com.bitejiuyeke.bitefileapi.file.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传结果
 */
@Data
public class FileVO implements Serializable {

    /**
     * 可访问 URL
     */
    private String url;

    /**
     * 路径信息，/目录/文件名.后缀名
     */
    private String key;

    /**
     * 原始文件名
     */
    private String name;
}
