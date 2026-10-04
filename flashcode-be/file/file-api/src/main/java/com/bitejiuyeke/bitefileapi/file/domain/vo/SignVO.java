package com.bitejiuyeke.bitefileapi.file.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * OSS 直传签名
 */
@Data
public class SignVO implements Serializable {

    /**
     * 签名
     */
    private String signature;

    private String host;

    private String pathPrefix;

    private String xOSSCredential;

    private String xOSSDate;

    private String policy;
}
