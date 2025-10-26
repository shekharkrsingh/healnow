package com.heal.doctor.Mail.impl;

import com.heal.doctor.Mail.IDoctorAccountMailService;
import com.heal.doctor.services.IEmailService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DoctorAccountMailServiceImpl implements IDoctorAccountMailService {
    @Value("${company.name}")
    private String companyName;

    private final IEmailService emailService;


    @Override
    public void doctorWelcomeMail(String doctorName, String email) {
        emailService.sendHtmlEmail(
                email,
                "Welcome to "+companyName+", Dr. "+doctorName+"!",
                "welcome.template.html",
                Map.of(
                        "companyName", companyName,
                        "doctorName", doctorName,
                        "dashboardUrl", "https://hportion.com"
                )
        );
    }

    @Override
    public void doctorPasswordChangeMail(String doctorName, String email) {
        LocalDateTime now = LocalDateTime.now();
        String formattedDate = now.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        String formattedTime = now.format(DateTimeFormatter.ofPattern("hh:mm a"));

        emailService.sendHtmlEmail(
                email,
                "Your Password Has Been Changed - " + companyName,
                "password-change.template.html",
                Map.of(
                        "companyName", companyName,
                        "userName", doctorName,
                        "changeDate", formattedDate,
                        "changeTime", formattedTime,
                        "securitySettingsUrl", "https://hportion.com/security-settings"
                )
        );
    }

    @Override
    public void doctorLoginEmailChangedMail(String receiverMail, String doctorName, String doctorOldEmail, String doctorNewEmail) {
        LocalDateTime now = LocalDateTime.now();
        String formattedDate = now.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        String formattedTime = now.format(DateTimeFormatter.ofPattern("hh:mm a"));

        emailService.sendHtmlEmail(
                receiverMail,
                "Account Email Updated - " + companyName,
                "email-change.template.html",
                Map.of(
                        "companyName", companyName,
                        "userName", doctorName,
                        "oldEmail", doctorOldEmail,
                        "newEmail", doctorNewEmail,
                        "changeDate", formattedDate,
                        "changeTime", formattedTime,
                        "loginUrl", "https://hportion.com/login"
                )
        );
    }

}
