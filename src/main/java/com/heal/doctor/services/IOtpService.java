package com.heal.doctor.services;

import com.heal.doctor.dto.OtpRequestDTO;
import com.heal.doctor.dto.OtpResponseDTO;

public interface IOtpService {
    OtpResponseDTO generateOtp(OtpRequestDTO otpRequestDTO);
    boolean validateOtp(String identifier, String otp);
}
