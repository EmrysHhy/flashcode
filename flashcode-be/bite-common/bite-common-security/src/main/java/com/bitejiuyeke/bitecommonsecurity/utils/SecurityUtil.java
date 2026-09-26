package com.bitejiuyeke.bitecommonsecurity.utils;

import com.bitejiuyeke.bitecommoncore.utils.ServletUtil;
import com.bitejiuyeke.bitecommondomain.constants.SecurityConstants;
import com.bitejiuyeke.bitecommondomain.constants.TokenConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

/**
 * 安全工具类
 */
public class SecurityUtil {

    /**
     * 获取请求token
     * @return token信息
     */
    public static String getToken() {
        return getToken(ServletUtil.getRequest());
    }

    /**
     * 根据request获取请求token
     * @param request 请求
     * @return token信息
     */
    public static String getToken(HttpServletRequest request) {
        String token = request.getHeader(SecurityConstants.AUTHENTICATION);
        return replaceTokenPrefix(token);
    }

    /**
     * 裁剪token前缀
     * @param token 前端可能设置了令牌的前缀
     * @return token信息
     */
    public static String replaceTokenPrefix(String token) {
        // 假如前端设置了令牌的前缀，需要替换裁剪
        if (StringUtils.isNotEmpty(token) && token.startsWith(TokenConstants.PREFIX)) {
            token = token.replaceFirst(TokenConstants.PREFIX, "");
        }
        return token;
    }

}
