package com.bitejiuyeke.bitecommonmessage.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * 邮件服务
 */
@Component
@Slf4j
@RefreshScope
public class EmailService {
    @Autowired
    private JavaMailSender emailSender;
    @Value("${email.username}")
    private String from;

    /**
     * 发送 HTML 邮件验证码
     *
     * @param to   收件人
     * @param code 验证码
     */
    public boolean sendEmail(String to, String code) {
        String subject = "登录验证码";
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(buildVerificationHtml(code), true);
            emailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("发送验证码邮件失败, to={}", to, e);
        }
        return false;
    }

    /**
     * 发送纯文本邮件
     *
     * @param to      收件人
     * @param subject 主题
     * @param text    正文
     */
    public boolean sendSimpleEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            emailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("发送邮件失败", e);
        }
        return false;
    }

    /**
     * 构建验证码 HTML
     * @param code 验证码
     * @return HTML 字符串
     */
    private static String buildVerificationHtml(String code) {
        String safeCode = code == null ? "" : code.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>登录验证码</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f4f5f7;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f5f7;padding:40px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:420px;background-color:#ffffff;border-radius:12px;padding:32px 28px;box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                          <tr>
                            <td align="center" style="padding-bottom:8px;">
                              <span style="font-size:20px;font-weight:600;color:#1a1a1a;">登录验证码</span>
                            </td>
                          </tr>
                          <tr>
                            <td align="center" style="padding-bottom:24px;">
                              <span style="font-size:14px;line-height:1.6;color:#666666;">您正在登录，请使用以下验证码完成验证。</span>
                            </td>
                          </tr>
                          <tr>
                            <td align="center" style="padding:20px 0;background-color:#f8f9fb;border-radius:8px;">
                              <span style="font-size:32px;font-weight:700;letter-spacing:8px;color:#2563eb;font-family:'Courier New',Courier,monospace;">%s</span>
                            </td>
                          </tr>
                          <tr>
                            <td align="center" style="padding-top:24px;">
                              <span style="font-size:12px;color:#999999;">验证码 5 分钟内有效，请勿泄露给他人。</span>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(safeCode);
    }
}
