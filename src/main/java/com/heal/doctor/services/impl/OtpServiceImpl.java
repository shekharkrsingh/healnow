package com.heal.doctor.services.impl;

import com.heal.doctor.dto.OtpRequestDTO;
import com.heal.doctor.dto.OtpResponseDTO;
import com.heal.doctor.models.OtpEntity;
import com.heal.doctor.repositories.OtpRepository;
import com.heal.doctor.services.IOtpService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;


@Service
public class OtpServiceImpl implements IOtpService {

    private final OtpRepository otpRepository;
    private final EmailServiceImpl emailService;

    @Value("${otp.length}")
    private int otpLength;

    @Value("${otp.expiration.minutes}")
    private int otpExpirationMinutes;

    private  final SecureRandom secureRandom=new SecureRandom();

    public OtpServiceImpl(OtpRepository otpRepository, EmailServiceImpl emailService) {
        this.otpRepository = otpRepository;
        this.emailService=emailService;
    }

    @Override
    public OtpResponseDTO generateOtp(OtpRequestDTO otpRequestDTO) {
        String otp=String.format("%0"+ otpLength + "d", secureRandom.nextInt((int)Math.pow(10,otpLength)));
        Date expirationTime = new Date(System.currentTimeMillis() + (long) otpExpirationMinutes * 60 * 1000);

        OtpEntity otpEntity=new OtpEntity(otpRequestDTO.getEmail(), otp, expirationTime);
        otpRepository.deleteByIdentifier(otpRequestDTO.getEmail());
        otpRepository.save(otpEntity);
        emailService.sendHtmlEmail(
                otpRequestDTO.getEmail(),
                "Test HTML Email",
                "email.template",
                Map.of("name", "Shekhar", "otp", otp, "message", "Use this OTP to proceed. It expires in " + otpExpirationMinutes + " minutes.")
        );
        return new OtpResponseDTO(otpRequestDTO.getEmail(), otp);
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
