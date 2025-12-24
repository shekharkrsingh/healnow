package com.heal.doctor.Mail;

public interface ICollaboratorMailService {
    void sendActivationEmail(String doctorName, String doctorEmail, String collaboratorName, String collaboratorEmail);
    void sendDeactivationEmail(String doctorName, String doctorEmail, String collaboratorName, String collaboratorEmail);
    void sendRemovalEmail(String doctorName, String doctorEmail, String collaboratorName, String collaboratorEmail);
}
