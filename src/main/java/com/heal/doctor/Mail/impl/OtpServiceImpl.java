package com.heal.doctor.Mail.impl;

import com.heal.doctor.Mail.IDoctorAccountMailService;
import com.heal.doctor.dto.OtpRequestDTO;
import com.heal.doctor.dto.OtpResponseDTO;
import com.heal.doctor.exception.BadRequestException;
import com.heal.doctor.exception.ResourceNotFoundException;
import com.heal.doctor.models.OtpEntity;
import com.heal.doctor.repositories.OtpRepository;
import com.heal.doctor.Mail.IOtpService;
import com.heal.doctor.services.IEmailService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements IOtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpServiceImpl.class);
    private static final int MILLISECONDS_PER_MINUTE = 60 * 1000;
    private static final int BASE_10 = 10;

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
        logger.info("Generating OTP for email: {}", otpRequestDTO.getEmail());
        String otp=String.format("%0"+ otpLength + "d", secureRandom.nextInt((int)Math.pow(BASE_10, otpLength)));
        Date expirationTime = new Date(System.currentTimeMillis() + (long) otpExpirationMinutes * MILLISECONDS_PER_MINUTE);

        OtpEntity otpEntity=new OtpEntity(otpRequestDTO.getEmail(), otp, expirationTime);
        otpRepository.deleteByIdentifier(otpRequestDTO.getEmail());
        otpRepository.save(otpEntity);
        logger.debug("OTP saved for email: {}, expires in {} minutes", otpRequestDTO.getEmail(), otpExpirationMinutes);
        
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
        ).exceptionally(ex -> {
            logger.error("Failed to send OTP email to: {}, error: {}", otpRequestDTO.getEmail(), ex.getMessage(), ex);
            return null;
        });
        logger.info("OTP email sending initiated asynchronously for: {}", otpRequestDTO.getEmail());

        return new OtpResponseDTO(otpRequestDTO.getEmail());
    }

    @Override
    public boolean validateOtp(String identifier, String otp) {
        logger.info("Validating OTP for identifier: {}", identifier);
        var latestOtpOptional=otpRepository.findTopByIdentifierOrderByCreatedAtDesc(identifier);

        if(latestOtpOptional.isEmpty()){
            logger.warn("OTP validation failed - OTP not found: identifier: {}", identifier);
            throw new ResourceNotFoundException("OTP", "OTP not found or has expired");
        }

        OtpEntity latestOtp=latestOtpOptional.get();

        if (new Date().after(latestOtp.getExpirationTime())) {
            logger.warn("OTP validation failed - expired: identifier: {}, expirationTime: {}", 
                    identifier, latestOtp.getExpirationTime());
            throw new BadRequestException("OTP has expired. Please request a new OTP");
        }

        if (!latestOtp.getOtp().equals(otp)) {
            logger.warn("OTP validation failed - invalid OTP: identifier: {}", identifier);
            throw new BadRequestException("Invalid OTP. Please check and try again");
        }
        otpRepository.delete(latestOtp);
        logger.info("OTP validated successfully: identifier: {}", identifier);
        return true;
    }
}
