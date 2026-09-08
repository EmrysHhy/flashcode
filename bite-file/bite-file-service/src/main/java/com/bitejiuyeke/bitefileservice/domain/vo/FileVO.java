package com.bitejiuyeke.bitefileservice.domain.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileVO {

    private String url;

    //路径信息   /目录/文件名.后缀名
    private String key;

    private String name;
}
