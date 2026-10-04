package com.bitejiuyeke.portalservice.flash.constants;

import com.bitejiuyeke.bitecommondomain.constants.CommonConstants;

/**
 *
 * @author Emrys
 * content:
 */
public class FlashcodeConstant extends CommonConstants {

    /**
     * 用户
     */
    public static final String USER_ID = "userId";

    /**
     * 多agent工作流
     */
    public static final String REQUIREMENT = "requirement";
    public static final String APP_ID = "appId";
    public static final String MODEL = "model";
    public static final String REFERENCE_PATH = "referencePath";
    public static final String FILES = "files";
    public static final String APP_IS_GENERATE = "appIsGenerate";
    public static final String APP_IS_BUILD = "appIsBuild";
    public static final String APP_IS_FIX = "appIsFix";
    public static final String APP_IS_SCREENSHOT = "appIsScreenshot";
    public static final String APP_IS_COMMIT = "appIsCommit";
    public static final String CODE_PATH = "codePath";
    public static final String APP_TYPE = "appType";
    public static final String ERROR_TYPE = "errorType";
    /** 是否属于改代码可修（npm/mvn/copy）；Docker、改库等为 false */
    public static final String ERROR_FIXABLE = "errorFixable";
    public static final String STAGE_NPM_INSTALL = "npm_install";
    public static final String STAGE_NPM_BUILD = "npm_build";
    public static final String STAGE_MVN_PACKAGE = "mvn_package";
    public static final String STAGE_COPY_PREVIEW = "copy_preview";
    public static final String STAGE_START_JAR = "start_jar";
    public static final String STAGE_UPDATE_URL = "update_url";
    public static final String STAGE_UNKNOWN = "unknown";
    public static final String GENERATE_ERROR_MESSAGE = "generateErrorMessage";
    public static final String PHOTO_PATH = "photoPath";
    public static final String BUILD_ERROR_MESSAGE = "buildErrorMessage";
    public static final String FIX_ERROR_MESSAGE = "fixErrorMessage";
    public static final String SCREENSHOT_ERROR_MESSAGE = "screenshotErrorMessage";
    public static final String COMMIT_ERROR_MESSAGE = "commitErrorMessage";
    /** 工作流重试计数（整次流程内） */
    public static final String GENERATE_ATTEMPT = "generateAttempt";
    public static final String SCREENSHOT_ATTEMPT = "screenshotAttempt";
    public static final String COMMIT_ATTEMPT = "commitAttempt";
    public static final String USER_CODE_DIR = "user-code";
    /** 用户上传的参考文件，与 generated 代码隔离，避免 saveCode 清空 */
    public static final String USER_REFERENCE_DIR = "user-reference";
    /**
     * 运行命令
     */
    public static final String NPM_REGISTRY = "https://registry.npmmirror.com";
    public static final String CMD_NPM_INSTALL =
            "npm install --registry=" + NPM_REGISTRY
                    + " vue@3.3.11 vue-router@4.2.5 vite@4.5.2 @vitejs/plugin-vue@4.5.2"
                    + " --save-exact --legacy-peer-deps";
    public static final String CMD_NPM_BUILD = "npm run build";
    public static final String CMD_MVN_PACKAGE = "mvn clean package -DskipTests";

    /**
     * 预览容器
     */

    public static final String USER_PREVIEW_DIR = "user-preview";
    //public static final String NGINX_PRE = "http://192.168.56.107:80/preview/";   //nacos上配置
    //public static final String CONTAINER_NAME = "flashcode-userapp-preview";  // nacos上配置
    public static final String PREVIEW_URL = "previewUrl";
    public static final String NGINX_UPDATE_SCRIPT = "/workspace/scripts/update_nginx_location.sh";
    /** 生成应用 Spring Boot 容器内端口 */
    public static final int JAR_HOST_PORT_BASE = 8001;
    public static final int JAR_HOST_PORT_RANGE = 1999;
    /**
     * 部署
     */
    public static final String FLASHCODE_USERAPP = "flashcode-userapp";
    /**
     * redis key
     */
    public static final String REDIS_CHAT_HISTORY_PRE = "chat_history_list_";  // 还需要加+appId



}
