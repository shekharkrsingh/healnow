package com.heal.doctor.Mail;

public interface IUserAccountEmailService {
    void passwordChangeMail(String doctorName, String email);
    void loginEmailChangedMail(String receiverMail, String doctorName, String doctorOldEmail, String doctorNewEmail );
}
