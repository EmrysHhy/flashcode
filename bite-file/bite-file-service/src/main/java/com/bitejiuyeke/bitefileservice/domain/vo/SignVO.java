package com.bitejiuyeke.bitefileservice.domain.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignVO {

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
