package com.bitejiuyeke.bitecommonsecurity.utils;

import com.bitejiuyeke.bitecommondomain.constants.SecurityConstants;
import com.bitejiuyeke.bitecommondomain.constants.TokenConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Map;

/**
 * Jwt工具类
 */
public class JwtUtil {

    /**
     * 令牌密钥
     */
    public static String secret = TokenConstants.SECRET;

    /**
     * 从原始数据声明生成令牌
     * @param claims 数据声明
     * @return 令牌
     */
    public static String createToken(Map<String, Object> claims) {
        return Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    /**
     * 根据令牌获取数据声明
     * @param token 令牌
     * @return 数据声明
     */
    public static Claims parseToken(String token) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }

    /**
     * 根据令牌获取用户标识
     * @param token 令牌
     * @return 用户标识
     */
    public static String getUserKey(String token) {
        Claims claims = parseToken(token);
        return getValue(claims, SecurityConstants.USER_KEY);
    }

    /**
     * 根据数据声明获取用户标识
     * @param claims 数据声明
     * @return 用户标识
     */
    public static String getUserKey(Claims claims) {
        return getValue(claims, SecurityConstants.USER_KEY);
    }

    /**
     * 根据令牌获取用户ID
     * @param token 令牌
     * @return 用户ID
     */
    public static String getUserId(String token) {
        Claims claims = parseToken(token);
        return getValue(claims, SecurityConstants.USER_ID);
    }

    /**
     * 根据数据声明获取用户ID
     * @param claims 数据声明
     * @return 用户ID
     */
    public static String getUserId(Claims claims) {
        return getValue(claims, SecurityConstants.USER_ID);
    }

    /**
     * 根据令牌获取用户名称
     * @param token 令牌
     * @return 用户名称
     */
    public static String getUserName(String token) {
        Claims claims = parseToken(token);
        return getValue(claims, SecurityConstants.USERNAME);
    }

    /**
     * 根据数据声明获取用户名称
     * @param claims 数据声明
     * @return 用户名称
     */
    public static String getUserName(Claims claims) {
        return getValue(claims, SecurityConstants.USERNAME);
    }

    /**
     * 根据令牌获取用户来源
     * @param token 令牌
     * @return 用户来源
     */
    public static String getUserFrom(String token) {
        Claims claims = parseToken(token);
        return getValue(claims, SecurityConstants.USER_FROM);
    }

    /**
     * 根据数据声明获取用户来源
     * @param claims 数据声明
     * @return 用户来源
     */
    public static String getUserFrom(Claims claims) {
        return getValue(claims, SecurityConstants.USER_FROM);
    }

    public static String getValue(Claims claims, String key) {
        Object value = claims.get(key);
        if (value == null) {
            return "";
        }
        return value.toString();
    }
}
