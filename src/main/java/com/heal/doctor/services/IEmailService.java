package com.heal.doctor.services;

import java.util.Map;

public interface IEmailService {
    void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables);

    void sendHtmlEmailWithAttachment(String to, String subject, String templateName,
                                     Map<String, Object> variables, byte[] attachment,
                                     String attachmentFileName, String attachmentContentType);

    void sendSimpleEmail(String to, String subject, String body);

    void sendSimpleEmailWithAttachment(String to, String subject, String body,
                                       byte[] attachment, String attachmentFileName,
                                       String attachmentContentType);
}