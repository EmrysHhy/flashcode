package com.bitejiuyeke.bitecommonmessage.service;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.bitejiuyeke.bitecommoncore.utils.JsonUtil;
import com.bitejiuyeke.bitecommondomain.constants.MessageConstants;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 短信服务
 */
@Component
@Slf4j
@RefreshScope
public class AliSmsService {

    /**
     * 客户端
     */
    @Autowired
    private Client client;

    /**
     * 短信模版代码
     */
    @Value("${sms.aliyun.templateCode:}")
    private String templateCode;

    /**
     * 签名
     */
    @Value("${sms.sing-name:}")
    private String singName;

    /**
     * 是否发送线上短信
     */
    @Value("${sms.send-message:false}")
    private boolean sendMessage;

    /**
     * 发送短信验证码
     * @param phone 手机号
     * @param code 验证码
     * @return 是否发送成功
     */
    public boolean sendMobileCode(String phone, String code) {
        Map<String, String> params = new HashMap<>();
        params.put("code", code);
        return sendTemMessage(phone, templateCode, params);
    }

    /**
     * 发送模版短信
     * @param phone 手机号
     * @param templateCode 模版编码
     * @param params 参数
     * @return 是否发送成功
     */
    private boolean sendTemMessage(String phone, String templateCode, Map<String, String> params) {
        // 1 把是否发送线上短信交给nacos做
        if (!sendMessage) {
            log.error("短信发送通道关闭, {}", phone);
            return false;
        }
        // 2 创建阿里云发送短信的请求
        SendSmsRequest sendSmsRequest = new SendSmsRequest();
        sendSmsRequest.setPhoneNumbers(phone);
        sendSmsRequest.setSignName(singName);
        sendSmsRequest.setTemplateCode(templateCode);
        sendSmsRequest.setTemplateParam(JsonUtil.obj2String(params));
        // 3 发送请求，根据结果判断是否发送成功
        try {
            SendSmsResponse sendSmsResponse = client.sendSms(sendSmsRequest);
            SendSmsResponseBody responseBody = sendSmsResponse.getBody();
            if (responseBody.getCode().equals(MessageConstants.SMS_MSG_OK)) {
                return true;
            }
            log.error("短信{} 发送失败, 失败原因{}...", new Gson().toJson(sendSmsRequest), responseBody.getMessage());
            return false;
        } catch (Exception e) {
            log.error("短信{} 发送失败, 失败原因{}...", new Gson().toJson(sendSmsRequest), e.getMessage());
            return false;
        }
    }
}
