package com.bitejiuyeke.bitecommonmessage.service;

import com.bitejiuyeke.bitecommoncore.utils.VerifyUtil;
import com.bitejiuyeke.bitecommondomain.constants.MessageConstants;
import com.bitejiuyeke.bitecommondomain.domain.ResultCode;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.bitecommonredis.service.RedisService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务
 */
@Service
@RefreshScope
public class CaptchaService {

    /**
     * 验证码与发送次数缓存
     */
    @Autowired
    private RedisService redisService;

    /**
     * 每日发送上限，配置项 captcha.send-limit
     */
    @Value("${captcha.send-limit:}")
    private Integer sendLimit;

    /**
     * 验证码有效期（分钟），配置项 captcha.code-expiration
     */
    @Value("${captcha.code-expiration:}")
    private Long phoneCodeExpiration;

    /**
     * 是否真实发送并使用随机码，配置项 captcha.send-message
     */
    @Value("${captcha.send-message:false}")
    private boolean sendMessage;

    /**
     * 短信发送通道
     */
    @Autowired
    private AliSmsService aliSmsService;

    /**
     * 邮件发送通道
     */
    @Autowired
    private EmailService emailService;

    /**
     * 发送验证码（手机号走短信，邮箱走邮件）
     *
     * @param account 手机号或邮箱
     * @return 验证码
     */
    public String sendCode(String account) {
        // 1 先校验手机号，再校验邮箱，格式都不对则抛异常
        boolean isPhone = VerifyUtil.checkPhone(account);
        boolean isEmail = !isPhone && VerifyUtil.checkEmail(account);
        if (!isPhone && !isEmail) {
            throw new ServiceException("手机号或邮箱格式错误", ResultCode.INVALID_PARA.getCode());
        }

        String limitCacheKey = (isPhone ? MessageConstants.SMS_CODE_TIMES_KEY : MessageConstants.EMAIL_CODE_TIMES_KEY) + account;
        String codeKey = (isPhone ? MessageConstants.SMS_CODE_KEY : MessageConstants.EMAIL_CODE_KEY) + account;

        // 2 按通道选择 Redis key，校验每日上限与 1 分钟内防刷
        Integer times = redisService.getCacheObject(limitCacheKey, Integer.class);
        times = times == null ? 0 : times;
        if (times >= sendLimit) {
            throw new ServiceException(ResultCode.SEND_MSG_FAILED);
        }

        String cacheValue = redisService.getCacheObject(codeKey, String.class);
        long expireTime = redisService.getExpire(codeKey);
        if (!StringUtils.isEmpty(cacheValue) && expireTime > phoneCodeExpiration * 60 - 60) {
            long time = expireTime - phoneCodeExpiration * 60 + 60;
            throw new ServiceException("操作频繁， 请在" + time + "秒之后再试", ResultCode.INVALID_PARA.getCode());
        }

        // 3 生成验证码；sendMessage 为 true 时手机走短信、邮箱走邮件
        String verifyCode = generateCode();

        if (sendMessage) {
            boolean result = isPhone
                    ? aliSmsService.sendMobileCode(account, verifyCode)
                    : emailService.sendEmail(account, verifyCode);
            if (!result) {
                throw new ServiceException(ResultCode.SEND_MSG_FAILED);
            }
        }

        // 4 写入验证码与当日发送次数
        redisService.setCacheObject(codeKey, verifyCode, phoneCodeExpiration, TimeUnit.MINUTES);
        long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(),
                LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
        redisService.setCacheObject(limitCacheKey, times + 1, seconds, TimeUnit.SECONDS);
        return verifyCode;
    }

    /**
     * 校验邮箱与验证码是否匹配
     *
     * @param email 邮箱
     * @param code  验证码
     * @return 是否匹配
     */
    public boolean checkEmailCode(String email, String code) {
        String cached = getEmailCode(email);
        if (cached == null || StringUtils.isEmpty(cached)) {
            throw new ServiceException(ResultCode.INVALID_CODE);
        }
        return cached.equals(code);
    }

    /**
     * 从缓存中获取邮箱的验证码
     *
     * @param email 邮箱
     * @return 验证码，不存在时返回 null
     */
    public String getEmailCode(String email) {
        return redisService.getCacheObject(MessageConstants.EMAIL_CODE_KEY + email, String.class);
    }

    /**
     * 从缓存中删除邮箱的验证码
     *
     * @param email 邮箱
     * @return 是否删除成功
     */
    public boolean deleteEmailCode(String email) {
        return redisService.deleteObject(MessageConstants.EMAIL_CODE_KEY + email);
    }

    /**
     * 生成验证码
     * sendMessage 为 true 时返回随机码，否则返回固定码
     *
     * @return 验证码
     */
    private String generateCode() {
        return sendMessage
                ? VerifyUtil.generateVerifyCode(MessageConstants.DEFAULT_SMS_LENGTH)
                : MessageConstants.DEFAULT_SMS_CODE;
    }

    /**
     * 从缓存中获取手机号的验证码
     *
     * @param phone 手机号
     * @return 验证码，不存在时返回 null
     */
    public String getCode(String phone) {
        return redisService.getCacheObject(MessageConstants.SMS_CODE_KEY + phone, String.class);
    }

    /**
     * 从缓存中删除手机号的验证码
     *
     * @param phone 手机号
     * @return 是否删除成功
     */
    public boolean deleteCode(String phone) {
        return redisService.deleteObject(MessageConstants.SMS_CODE_KEY + phone);
    }

    /**
     * 校验手机号与验证码是否匹配
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 是否匹配
     */
    public boolean checkCode(String phone, String code) {
        String cached = getCode(phone);
        if (cached == null || StringUtils.isEmpty(cached)) {
            throw new ServiceException(ResultCode.INVALID_CODE);
        }
        return cached.equals(code);
    }
}
