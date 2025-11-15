package com.heal.doctor.services.impl;

import com.heal.doctor.exception.EmailException;
import com.heal.doctor.services.IEmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class EmailServiceImpl implements IEmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Autowired
    public EmailServiceImpl(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Override
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        logger.debug("Sending HTML email: to: {}, subject: {}, template: {}", to, subject, templateName);
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom(senderEmail);

            mailSender.send(message);
            logger.info("HTML email sent successfully: to: {}, subject: {}", to, subject);
        } catch (MessagingException e) {
            logger.error("Failed to send HTML email: to: {}, subject: {}, error: {}", to, subject, e.getMessage(), e);
            throw new EmailException("Failed to send email to " + to + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void sendHtmlEmailWithAttachment(String to, String subject, String templateName,
                                            Map<String, Object> variables, byte[] attachment,
                                            String attachmentFileName, String attachmentContentType) {
        logger.debug("Sending HTML email with attachment: to: {}, subject: {}, attachment: {}", 
                to, subject, attachmentFileName);
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom(senderEmail);

            ByteArrayResource pdfResource = new ByteArrayResource(attachment);
            helper.addAttachment(attachmentFileName, pdfResource, attachmentContentType);

            mailSender.send(message);
            logger.info("HTML email with attachment sent successfully: to: {}, subject: {}, attachment: {}", 
                    to, subject, attachmentFileName);
        } catch (MessagingException e) {
            logger.error("Failed to send HTML email with attachment: to: {}, subject: {}, attachment: {}, error: {}", 
                    to, subject, attachmentFileName, e.getMessage(), e);
            throw new EmailException("Failed to send email with attachment to " + to + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void sendSimpleEmail(String to, String subject, String body) {
        logger.debug("Sending simple email: to: {}, subject: {}", to, subject);
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            helper.setFrom(senderEmail);
            mailSender.send(message);
            logger.info("Simple email sent successfully: to: {}, subject: {}", to, subject);
        } catch (MessagingException e) {
            logger.error("Failed to send simple email: to: {}, subject: {}, error: {}", to, subject, e.getMessage(), e);
            throw new EmailException("Failed to send email to " + to + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void sendSimpleEmailWithAttachment(String to, String subject, String body,
                                              byte[] attachment, String attachmentFileName,
                                              String attachmentContentType) {
        logger.debug("Sending simple email with attachment: to: {}, subject: {}, attachment: {}", 
                to, subject, attachmentFileName);
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            helper.setFrom(senderEmail);

            ByteArrayResource pdfResource = new ByteArrayResource(attachment);
            helper.addAttachment(attachmentFileName, pdfResource, attachmentContentType);

            mailSender.send(message);
            logger.info("Simple email with attachment sent successfully: to: {}, subject: {}, attachment: {}", 
                    to, subject, attachmentFileName);
        } catch (MessagingException e) {
            logger.error("Failed to send simple email with attachment: to: {}, subject: {}, attachment: {}, error: {}", 
                    to, subject, attachmentFileName, e.getMessage(), e);
            throw new EmailException("Failed to send email with attachment to " + to + ": " + e.getMessage(), e);
        }
    }
}