package com.heal.doctor.services.impl;

import com.heal.doctor.services.IEmailService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class EmailServiceImpl implements IEmailService {

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
        var message=mailSender.createMimeMessage();
        try{
            var helper= new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            Context context=new Context();
            context.setVariables(variables);
            String htmlContent=templateEngine.process(templateName, context);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom(senderEmail);

            mailSender.send(message);
        }catch(MessagingException e){
            throw new RuntimeException("Error sending email: "+ e.getMessage());
        }
    }

    @Override
    public void sendSimpleEmail(String to, String subject, String body){
        var message=mailSender.createMimeMessage();
        try{
            var helper =new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            helper.setFrom(senderEmail);
            mailSender.send(message);
        } catch (MessagingException e){
            throw  new RuntimeException("Error sending email: " + e.getMessage());
        }
    }
}
