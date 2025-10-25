package com.heal.doctor.Mail.impl;

import com.heal.doctor.Mail.IDoctorAccountMailService;
import com.heal.doctor.dto.OtpRequestDTO;
import com.heal.doctor.dto.OtpResponseDTO;
import com.heal.doctor.models.OtpEntity;
import com.heal.doctor.repositories.OtpRepository;
import com.heal.doctor.Mail.IOtpService;
import com.heal.doctor.services.IEmailService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements IOtpService {

    private final OtpRepository otpRepository;
    private final IEmailService emailService;

    @Value("${otp.length}")
    private int otpLength;

    @Value("${company.name}")
    private String companyName;

    @Value("${otp.expiration.minutes}")
    private int otpExpirationMinutes;

    private  final SecureRandom secureRandom=new SecureRandom();

    @Override
    public OtpResponseDTO generateOtp(OtpRequestDTO otpRequestDTO) {
        String otp=String.format("%0"+ otpLength + "d", secureRandom.nextInt((int)Math.pow(10,otpLength)));
        Date expirationTime = new Date(System.currentTimeMillis() + (long) otpExpirationMinutes * 60 * 1000);

        OtpEntity otpEntity=new OtpEntity(otpRequestDTO.getEmail(), otp, expirationTime);
        otpRepository.deleteByIdentifier(otpRequestDTO.getEmail());
        otpRepository.save(otpEntity);
        emailService.sendHtmlEmail(
                otpRequestDTO.getEmail(),
                "Your One-Time Password (OTP)",
                "email.template",
                Map.of(
                        "companyName", companyName,
                        "otp", otp,
                        "message", "Use the OTP below to complete your verification. This OTP will expire in "
                                + otpExpirationMinutes + " minutes. Please do not share it with anyone."
                )
        );

        return new OtpResponseDTO(otpRequestDTO.getEmail());
    }

    @Override
    public boolean validateOtp(String identifier, String otp) {
        var latestOtpOptional=otpRepository.findTopByIdentifierOrderByCreatedAtDesc(identifier);

        if(latestOtpOptional.isEmpty()){
            throw new RuntimeException("Otp either expired or not available");
        }

        OtpEntity latestOtp=latestOtpOptional.get();

        if (new Date().after(latestOtp.getExpirationTime())) {
            throw new RuntimeException("OTP has expired");
        }


        if (!latestOtp.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }
        otpRepository.delete(latestOtp);
        return true;
    }
}
