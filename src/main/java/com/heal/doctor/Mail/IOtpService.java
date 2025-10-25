package com.heal.doctor.Mail;

import com.heal.doctor.dto.OtpRequestDTO;
import com.heal.doctor.dto.OtpResponseDTO;

public interface IOtpService {
    OtpResponseDTO generateOtp(OtpRequestDTO otpRequestDTO);
    boolean validateOtp(String identifier, String otp);
}
