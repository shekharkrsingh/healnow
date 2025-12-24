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

}
