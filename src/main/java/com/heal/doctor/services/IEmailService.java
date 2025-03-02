package com.heal.doctor.services;

import java.util.Map;

public interface IEmailService {
    void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables);
    void sendSimpleEmail(String to, String subject, String body);
}
