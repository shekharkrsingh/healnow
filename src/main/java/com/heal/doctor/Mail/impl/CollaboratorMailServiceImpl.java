package com.heal.doctor.Mail.impl;

import com.heal.doctor.Mail.ICollaboratorMailService;
import com.heal.doctor.services.IEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CollaboratorMailServiceImpl implements ICollaboratorMailService {

    private final IEmailService emailService;

    @Value("${company.name:HealNow}")
    private String companyName;

    @Override
    public void sendActivationEmail(String doctorName, String doctorEmail, String collaboratorName, String collaboratorEmail) {
        String subject = "Collaborator Account Activated - " + companyName;
        Map<String, Object> vars = Map.of(
                "doctorName", doctorName,
                "collaboratorName", collaboratorName,
                "status", "ACTIVATED",
                "companyName", companyName
        );
        emailService.sendHtmlEmail(collaboratorEmail, subject, "collaborator-status-change.template.html", vars);
        emailService.sendHtmlEmail(doctorEmail, subject, "collaborator-status-change.template.html", vars);
    }

    @Override
    public void sendDeactivationEmail(String doctorName, String doctorEmail, String collaboratorName, String collaboratorEmail) {
        String subject = "Collaborator Account Deactivated - " + companyName;
        Map<String, Object> vars = Map.of(
                "doctorName", doctorName,
                "collaboratorName", collaboratorName,
                "status", "DEACTIVATED",
                "companyName", companyName
        );
        emailService.sendHtmlEmail(collaboratorEmail, subject, "collaborator-status-change.template.html", vars);
        emailService.sendHtmlEmail(doctorEmail, subject, "collaborator-status-change.template.html", vars);
    }

    @Override
    public void sendRemovalEmail(String doctorName, String doctorEmail, String collaboratorName, String collaboratorEmail) {
        String subject = "Collaborator Account Removed - " + companyName;
        Map<String, Object> vars = Map.of(
                "doctorName", doctorName,
                "collaboratorName", collaboratorName,
                "status", "REMOVED",
                "companyName", companyName
        );
        emailService.sendHtmlEmail(collaboratorEmail, subject, "collaborator-status-change.template.html", vars);
        emailService.sendHtmlEmail(doctorEmail, subject, "collaborator-status-change.template.html", vars);
    }
}
