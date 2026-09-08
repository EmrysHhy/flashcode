package com.bitejiuyeke.bitecommonsecurity.service;

import com.bitejiuyeke.bitecommoncore.utils.ServletUtil;
import com.bitejiuyeke.bitecommondomain.constants.CacheConstants;
import com.bitejiuyeke.bitecommondomain.constants.SecurityConstants;
import com.bitejiuyeke.bitecommondomain.constants.TokenConstants;
import com.bitejiuyeke.bitecommonredis.service.RedisService;
import com.bitejiuyeke.bitecommonsecurity.domain.dto.LoginUserDTO;
import com.bitejiuyeke.bitecommonsecurity.domain.dto.TokenDTO;
import com.bitejiuyeke.bitecommonsecurity.utils.JwtUtil;
import com.bitejiuyeke.bitecommonsecurity.utils.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * token服务类
 */
@Component
public class TokenService {

    /**
     * 毫秒
     */
    private final static long MILLIS_SECOND = 1000;

    /**
     * 分钟
     */
    private final static long MILLIS_MINUTE = 60 * MILLIS_SECOND;

    /**
     * 过期时间
     */
    private final static Long EXPIRE_TIME = CacheConstants.EXPIRATION;

    /**
     * token的KEY前缀
     */
    private final static String ACCESS_TOKEN = TokenConstants.LOGIN_TOKEN_KEY;

    /**
     * 120分钟
     */
    private final static Long MILLIS_MINUTE_TEN = CacheConstants.REFRESH_TIME * MILLIS_MINUTE;

    /**
     * 引用redis服务
     */
    @Autowired
    private RedisService redisService;

    /**
     * 创建token
     * @param loginUserDTO 登录信息
     * @return token信息
     */
    public TokenDTO createToken(LoginUserDTO loginUserDTO) {
        // 1 随机产生用户标识
        String token = UUID.randomUUID().toString();
        loginUserDTO.setToken(token);
        refreshToken(loginUserDTO);
        // 2 生成原始数据声明
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put(SecurityConstants.USER_KEY, token);
        claimsMap.put(SecurityConstants.USER_ID, loginUserDTO.getUserId());
        claimsMap.put(SecurityConstants.USERNAME, loginUserDTO.getUserName());
        claimsMap.put(SecurityConstants.USER_FROM, loginUserDTO.getUserFrom());
        // 3 生成TokenDTO
        TokenDTO tokenDTO = new TokenDTO();
        tokenDTO.setAccessToken(JwtUtil.createToken(claimsMap));
        tokenDTO.setExpires(EXPIRE_TIME);
        return tokenDTO;
    }

    /**
     * 根据令牌获取用户信息
     * @param token 令牌
     * @return 用户信息
     */
    public LoginUserDTO getLoginUser(String token) {
        // 1 初始化用户信息
        LoginUserDTO user = null;
        // 2 解析令牌获取用户信息
        try {
            if (StringUtils.isNotEmpty(token)) {
                String userKey = JwtUtil.getUserKey(token);
                user = redisService.getCacheObject(getTokenKey(userKey), LoginUserDTO.class);
                return user;
            }
        } catch (Exception e) {
        }
        // 3 返回user
        return user;
    }

    /**
     * 根据请求来获取用户信息
     * @param request 请求
     * @return 用户信息
     */
    public LoginUserDTO getLoginUser(HttpServletRequest request) {
        String token = SecurityUtil.getToken(request);
        return getLoginUser(token);
    }

    /**
     * 不传参数获取用户信息
     * @return 用户信息
     */
    public LoginUserDTO getLoginUser() {
        return getLoginUser(ServletUtil.getRequest());
    }

    /**
     * 根据令牌删除用户登录态
     * @param token 令牌
     */
    public void delLoginUser(String token) {
        if (StringUtils.isNotEmpty(token)) {
            String useKey = JwtUtil.getUserKey(token);
            redisService.deleteObject(getTokenKey(useKey));
        }
    }

    /**
     * 允许超管删除别人的登录状态
     * @param userId 用户ID
     * @param userFrom 用户来源
     */
    public void delLoginUser(Long userId, String userFrom) {
        if (userId == null) return;
        // 遍历redis里面的key删除
        Collection<String> tokenKeys = redisService.keys(ACCESS_TOKEN + "*");
        for (String tokenKey : tokenKeys) {
            LoginUserDTO user = redisService.getCacheObject(tokenKey, LoginUserDTO.class);
            if (user != null && user.getUserId().equals(userId) && user.getUserFrom().equals(userFrom)) {
                redisService.deleteObject(tokenKey);
            }
        }
    }

    /**
     * 验证令牌有效期   不到120分钟  自动刷新下
     * @param loginUserDTO 用户信息
     */
    public void verifyToken(LoginUserDTO loginUserDTO) {
        long expireTime = loginUserDTO.getExpireTime();
        long currentTime = System.currentTimeMillis();
        if (expireTime - currentTime <= MILLIS_MINUTE_TEN) {
            refreshToken(loginUserDTO);
        }
    }

    /**
     * 设置用户身份信息，允许登录
     * @param loginUserDTO 用户信息
     */
    public void setLoginUser(LoginUserDTO loginUserDTO) {
        if (loginUserDTO != null && StringUtils.isNotEmpty(loginUserDTO.getToken())) {
            refreshToken(loginUserDTO);
        }
    }

    /**
     * 缓存用户信息设置令牌有效期
     * @param loginUserDTO 用户信息
     */
    public void refreshToken(LoginUserDTO loginUserDTO) {
        loginUserDTO.setLoginTime(System.currentTimeMillis());
        loginUserDTO.setExpireTime(loginUserDTO.getLoginTime() + EXPIRE_TIME * MILLIS_MINUTE);
        // 根据随机产生用户标识生成key
        String userKey = getTokenKey(loginUserDTO.getToken());
        // 生成loginUserDTO缓存
        redisService.setCacheObject(userKey, loginUserDTO, EXPIRE_TIME, TimeUnit.MINUTES);
    }

    /**
     * 获取token key的信息
     * @param token token
     * @return tokenKey
     */
    public String getTokenKey(String token) {
        return ACCESS_TOKEN + token;
    }
}
