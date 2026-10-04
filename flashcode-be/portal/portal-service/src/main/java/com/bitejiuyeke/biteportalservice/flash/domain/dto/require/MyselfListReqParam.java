package com.bitejiuyeke.biteportalservice.flash.domain.dto.require;

import com.bitejiuyeke.bitecommondomain.domain.dto.BasePageReqDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 我的应用列表。userId 由服务端按登录用户写入，请求体只传分页和可选类型。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MyselfListReqParam extends BasePageReqDTO implements Serializable {
    private Long userId;
    private Integer appType;
}
