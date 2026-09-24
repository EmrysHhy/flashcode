package com.bitejiuyeke.bitecommonmessage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * 邮件服务配置参数
 */
@Configuration
@RefreshScope
public class EmailConfig {

    /**
     * 邮件服务器地址
     */
    @Value("${email.host}")
    private String host;

    /**
     * 邮件服务器端口
     */
    @Value("${email.port}")
    private Integer port;

    /**
     * 发件人邮箱
     */
    @Value("${email.username}")
    private String username;

    /**
     * 发件人邮箱授权码
     */
    @Value("${email.password}")
    private String password;

    /**
     * 连接超时时间
     */
    @Value("${email.connection-timeout}")
    private int connectionTimeout;

    /**
     * 读取超时时间
     */
    @Value("${email.timeout}")
    private int timeout;

    /**
     * 写超时时间
     */
    @Value("${email.write-timeout}")
    private int writeTimeout;

    /**
     * 注册邮件发送器
     * @return JavaMailSender
     */
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setHost(host);
        javaMailSender.setPort(port);
        javaMailSender.setProtocol("smtp");
        javaMailSender.setDefaultEncoding("utf-8");
        javaMailSender.setUsername(username);
        javaMailSender.setPassword(password);

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", true);

        // 根据端口选择加密方式：587端口使用STARTTLS，465端口使用SSL
        if (port == 465) {
            // 465端口使用SSL
            properties.put("mail.smtp.ssl.enable", true);
            properties.put("mail.smtp.ssl.required", true);
            properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.put("mail.smtp.socketFactory.port", 465);
        } else {
            // 587端口使用STARTTLS (TLS)
            properties.put("mail.smtp.starttls.enable", true);
            properties.put("mail.smtp.starttls.required", true);
        }

        // 添加超时和连接配置
        properties.put("mail.smtp.connectiontimeout", connectionTimeout);
        properties.put("mail.smtp.timeout", timeout);
        properties.put("mail.smtp.writetimeout", writeTimeout);

        javaMailSender.setJavaMailProperties(properties);

        return javaMailSender;
    }
}