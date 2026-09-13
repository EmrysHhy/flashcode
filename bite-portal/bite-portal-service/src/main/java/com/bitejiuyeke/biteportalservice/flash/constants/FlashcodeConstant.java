package com.bitejiuyeke.biteportalservice.flash.constants;

/**
 *
 * @author Emrys
 * content:
 */
public class FlashcodeConstant {
    public static final String APP_DOC = "appDoc";
    public static final String APP_NAME = "appName";
    public static final String APP_DESC = "appDesc";
    public static final String USER_ID = "userId";
    public static final String APP_TYPE = "appType";
    public static final String USER_CODE_DIR = "user-code";
    public static final String USER_PREVIEW_DIR = "user-preview";
    public static final String NGINX_PRE = "http://192.168.56.107:80/preview/";   //todo nacos上配置
    /**
     * 运行命令
     */
    public static final String CMD_NPM_INSTALL = "npm install";
    public static final String CMD_NPM_BUILD = "npm run build";
    public static final String CMD_MVN_PACKAGE = "mvn clean package";

    /**
     * 预览容器
     */
    public static final String CONTAINER_NAME = "flashcode-userapp-preview";  //todo nacos上配置
    public static final String NGINX_UPDATE_SCRIPT = "/workspace/scripts/update_nginx_location.sh";
    /** 生成应用 Spring Boot 容器内端口 */
    public static final int JAR_CONTAINER_PORT = 8080;
    public static final int JAR_HOST_PORT_BASE = 8001;
    public static final int JAR_HOST_PORT_RANGE = 1999;
    public static final String USER_DEVELOP_DIR = "user-develop";
}
