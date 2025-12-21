package com.heal.doctor.Mail.impl;

import com.heal.doctor.Mail.ICollaboratorMailService;
import com.heal.doctor.services.IEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CollaboratorMailServiceImpl implements ICollaboratorMailService {

    private final IEmailService emailService;

    @Value("${company.name:HealNow}")
    private String companyName;

    @Override
    public void sendActivationEmail(String doctorName, String doctorEmail, String collaboratorName, String collaboratorEmail) {
        LocalDateTime now = LocalDateTime.now();
        String formattedDate = now.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        String formattedTime = now.format(DateTimeFormatter.ofPattern("hh:mm a"));

        String subject = "Collaborator Account Activated - " + companyName;
        Map<String, Object> vars = Map.of(
                "changeDate", formattedDate,
                "changeTime", formattedTime,
                "doctorName", doctorName,
                "collaboratorName", collaboratorName,
                "status", "ACTIVATED",
                "companyName", companyName
        );
        emailService.sendHtmlEmail(collaboratorEmail, subject, "collaborator-status-change.template.html", vars);
//        emailService.sendHtmlEmail(doctorEmail, subject, "collaborator-status-change.template.html", vars);
    }

    @Override
    public void sendDeactivationEmail(String doctorName, String doctorEmail, String collaboratorName, String collaboratorEmail) {
        LocalDateTime now = LocalDateTime.now();
        String formattedDate = now.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        String formattedTime = now.format(DateTimeFormatter.ofPattern("hh:mm a"));

        String subject = "Collaborator Account Deactivated - " + companyName;
        Map<String, Object> vars = Map.of(
                "changeDate", formattedDate,
                "changeTime", formattedTime,
                "doctorName", doctorName,
                "collaboratorName", collaboratorName,
                "status", "DEACTIVATED",
                "companyName", companyName
        );
        emailService.sendHtmlEmail(collaboratorEmail, subject, "collaborator-status-change.template.html", vars);
//        emailService.sendHtmlEmail(doctorEmail, subject, "collaborator-status-change.template.html", vars);
    }

    @Override
    public void sendRemovalEmail(String doctorName, String doctorEmail, String collaboratorName, String collaboratorEmail) {
        LocalDateTime now = LocalDateTime.now();
        String formattedDate = now.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        String formattedTime = now.format(DateTimeFormatter.ofPattern("hh:mm a"));

        String subject = "Collaborator Account Removed - " + companyName;
        Map<String, Object> vars = Map.of(
                "changeDate", formattedDate,
                "changeTime", formattedTime,
                "doctorName", doctorName,
                "collaboratorName", collaboratorName,
                "status", "REMOVED",
                "companyName", companyName
        );
        emailService.sendHtmlEmail(collaboratorEmail, subject, "collaborator-status-change.template.html", vars);
//        emailService.sendHtmlEmail(doctorEmail, subject, "collaborator-status-change.template.html", vars);
    }
}
