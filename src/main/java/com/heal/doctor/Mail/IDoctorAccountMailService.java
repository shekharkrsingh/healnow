package com.heal.doctor.Mail;

import org.springframework.stereotype.Service;

@Service
public interface IDoctorAccountMailService {
    void doctorWelcomeMail(String doctorName, String email);
    void doctorPasswordChangeMail(String doctorName, String email);
    void doctorLoginEmailChangedMail(String reciverMail, String doctorName, String doctorOldEmail, String doctorNewEmail );
}
