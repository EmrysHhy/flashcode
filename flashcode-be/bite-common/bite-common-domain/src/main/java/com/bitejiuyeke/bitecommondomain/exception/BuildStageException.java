package com.bitejiuyeke.bitecommondomain.exception;

import lombok.Getter;

/**
 * 预览构建某一步失败。stage 给模型和路由用，message 尽量保留命令原文。
 */
@Getter
public class BuildStageException extends RuntimeException {

    private final String stage;

    private final boolean fixable;

    public BuildStageException(String stage, boolean fixable, String message, Throwable cause) {
        super(message, cause);
        this.stage = stage;
        this.fixable = fixable;
    }
}
