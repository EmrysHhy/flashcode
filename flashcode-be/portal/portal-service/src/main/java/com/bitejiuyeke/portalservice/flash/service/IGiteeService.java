package com.bitejiuyeke.portalservice.flash.service;

import java.util.Map;

/**
 * 通过 Gitee Open API 推拉生成应用的源码（仓库内按 appId 分子目录）。
 */
public interface IGiteeService {

    /**
     * 把解析出的源码文件提交到 {repo}/{appId}/...，其它 appId 目录不受影响。
     */
    void push(Long appId, Map<String, String> files);

    /**
     * 从仓库拉取 {appId}/ 下的源码，覆盖写入本地 user-code/{appId}。
     */
    void pull(Long appId);
}
