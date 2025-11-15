package com.heal.doctor.services;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface IEmailService {
    CompletableFuture<Void> sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables);

    CompletableFuture<Void> sendHtmlEmailWithAttachment(String to, String subject, String templateName,
                                     Map<String, Object> variables, byte[] attachment,
                                     String attachmentFileName, String attachmentContentType);

    CompletableFuture<Void> sendSimpleEmail(String to, String subject, String body);

    CompletableFuture<Void> sendSimpleEmailWithAttachment(String to, String subject, String body,
                                       byte[] attachment, String attachmentFileName,
                                       String attachmentContentType);
}