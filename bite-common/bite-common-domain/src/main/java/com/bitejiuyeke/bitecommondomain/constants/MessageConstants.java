package com.bitejiuyeke.bitecommondomain.constants;

/**
 * 发送短信的常量
 */
public class MessageConstants {

    /**
     * 发送短信成功的响应码
     */
    public static final String SMS_MSG_OK = "OK";

    /**
     * 短信发送次数的key
     */
    public static final String SMS_CODE_TIMES_KEY = "sms:times:";

    /**
     * 短信验证码频繁发送的key
     */
    public static final String SMS_CODE_KEY = "sms:code:";

    /**
     * 邮件发送次数的 key
     */
    public static final String EMAIL_CODE_TIMES_KEY = "email:times:";

    /**
     * 邮件验证码的 key
     */
    public static final String EMAIL_CODE_KEY = "email:code:";

    /**
     * 默认验证码的长度
     */
    public static final int DEFAULT_SMS_LENGTH = 6;

    /**
     * 默认验证码
     */
    public static final String DEFAULT_SMS_CODE = "123456";
}
