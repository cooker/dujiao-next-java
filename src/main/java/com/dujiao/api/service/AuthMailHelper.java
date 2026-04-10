package com.dujiao.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class AuthMailHelper {

    private static final Logger log = LoggerFactory.getLogger(AuthMailHelper.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public AuthMailHelper(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${dujiao.mail.from:}") String fromAddress) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.fromAddress = fromAddress;
    }

    public void sendVerificationCode(String toEmail, String code, String purposeLabel) {
        String subject = "验证码";
        String body = "您的验证码是：" + code + "（" + purposeLabel + "，15 分钟内有效）";
        if (mailSender == null || fromAddress == null || fromAddress.isBlank()) {
            log.info("[verify-code] to={} purpose={} code={}", toEmail, purposeLabel, code);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(toEmail);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("send mail failed, fallback log: to={} code={}", toEmail, code, e);
            log.info("[verify-code] to={} purpose={} code={}", toEmail, purposeLabel, code);
        }
    }

    /** 管理后台 SMTP 测试发送。返回 true 表示已尝试通过 SMTP 发送。 */
    public boolean sendCustomEmail(String toEmail, String subject, String body) {
        String subj = subject == null || subject.isBlank() ? "SMTP 测试邮件" : subject.trim();
        String content = body == null || body.isBlank() ? "这是一封测试邮件。" : body;
        if (mailSender == null || fromAddress == null || fromAddress.isBlank()) {
            log.info("[smtp-test] to={} subject={} body={}", toEmail, subj, content);
            return false;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(toEmail);
            msg.setSubject(subj);
            msg.setText(content);
            mailSender.send(msg);
            return true;
        } catch (Exception e) {
            log.warn("send smtp test mail failed, fallback log: to={}", toEmail, e);
            log.info("[smtp-test] to={} subject={} body={}", toEmail, subj, content);
            return false;
        }
    }
}
