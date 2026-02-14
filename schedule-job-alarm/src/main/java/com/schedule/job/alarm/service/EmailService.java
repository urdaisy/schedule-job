package com.schedule.job.alarm.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 邮件发送服务
 */
@Slf4j
@Service
public class EmailService {
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.from:}")
    private String fromEmail;
    
    /**
     * 开发模式：是否使用日志输出替代真实邮件发送
     * 设置为 true 时，不会实际发送邮件，而是将邮件内容输出到日志中
     * 适用于开发/测试环境，无需配置真实的邮件服务器
     */
    @Value("${alert.email.dev-mode:true}")
    private boolean devMode;
    
    /**
     * 发送告警邮件
     * 
     * @param toEmails 收件人邮箱列表（多个邮箱用逗号分隔）
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 是否发送成功
     */
    public boolean sendAlertEmail(String toEmails, String subject, String content) {
        if (toEmails == null || toEmails.trim().isEmpty()) {
            log.warn("收件人邮箱为空，无法发送邮件");
            return false;
        }
        
        // 开发模式：使用日志输出替代真实邮件发送
        if (devMode) {
            log.info("========== 告警邮件（开发模式 - 未实际发送） ==========");
            log.info("收件人: {}", toEmails);
            log.info("主题: {}", subject);
            log.info("发件人: {}", fromEmail != null && !fromEmail.isEmpty() ? fromEmail : "未配置");
            log.info("邮件内容:");
            log.info("{}", content);
            log.info("================================================");
            return true;
        }
        
        // 生产模式：实际发送邮件
        if (mailSender == null) {
            log.warn("JavaMailSender未配置，无法发送邮件。如需发送邮件，请配置spring.mail相关属性，或设置alert.email.dev-mode=true使用开发模式");
            return false;
        }
        
        try {
            // 解析收件人列表
            List<String> emailList = Arrays.asList(toEmails.split(","));
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // 设置发件人
            if (fromEmail != null && !fromEmail.isEmpty()) {
                helper.setFrom(fromEmail);
            }
            
            // 设置收件人
            helper.setTo(emailList.toArray(new String[0]));
            
            // 设置主题
            helper.setSubject(subject);
            
            // 设置内容（HTML格式）
            helper.setText(content, true);
            
            // 发送邮件
            mailSender.send(message);
            
            log.info("告警邮件发送成功，收件人: {}, 主题: {}", toEmails, subject);
            return true;
            
        } catch (MessagingException e) {
            log.error("发送告警邮件失败，收件人: {}, 主题: {}", toEmails, subject, e);
            return false;
        } catch (Exception e) {
            log.error("发送告警邮件异常，收件人: {}, 主题: {}", toEmails, subject, e);
            return false;
        }
    }
    
    /**
     * 构建任务失败告警邮件内容
     * 
     * @param jobName 任务名称
     * @param jobGroup 任务组
     * @param errorMsg 错误信息
     * @param executeTime 执行时间
     * @return HTML格式的邮件内容
     */
    public String buildAlertEmailContent(String jobName, String jobGroup, String errorMsg, String executeTime) {
        StringBuilder content = new StringBuilder();
        content.append("<html><body style='font-family: Arial, sans-serif;'>");
        content.append("<h2 style='color: #d32f2f;'>任务执行失败告警</h2>");
        content.append("<div style='background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 20px 0;'>");
        content.append("<p><strong>任务名称：</strong>").append(jobName).append("</p>");
        content.append("<p><strong>任务组：</strong>").append(jobGroup).append("</p>");
        content.append("<p><strong>执行时间：</strong>").append(executeTime).append("</p>");
        content.append("<p><strong>错误信息：</strong></p>");
        content.append("<pre style='background-color: #fff; padding: 10px; border-left: 3px solid #d32f2f; white-space: pre-wrap; word-wrap: break-word;'>")
               .append(errorMsg != null ? errorMsg : "未知错误")
               .append("</pre>");
        content.append("</div>");
        content.append("<p style='color: #666; font-size: 12px;'>此邮件由分布式调度任务平台自动发送，请勿回复。</p>");
        content.append("</body></html>");
        return content.toString();
    }
}
