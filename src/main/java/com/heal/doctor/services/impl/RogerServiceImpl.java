package com.heal.doctor.services.impl;

import com.heal.doctor.dto.LoginResponseDTO;
import com.heal.doctor.dto.RogerProfileDTO;
import com.heal.doctor.dto.RogerRegistrationDTO;
import com.heal.doctor.exception.ConflictException;
import com.heal.doctor.exception.ResourceNotFoundException;
import com.heal.doctor.models.RogerEntity;
import com.heal.doctor.models.UserEntity;
import com.heal.doctor.models.enums.RolesEnum;
import com.heal.doctor.repositories.RogerRepository;
import com.heal.doctor.repositories.UserRepository;
import com.heal.doctor.services.IRogerService;
import com.heal.doctor.services.IUserService;
import com.heal.doctor.Mail.IOtpService;
import com.heal.doctor.utils.CurrentUserName;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class RogerServiceImpl implements IRogerService {

    private static final Logger logger = LoggerFactory.getLogger(RogerServiceImpl.class);

    private final UserRepository userRepository;
    private final RogerRepository rogerRepository;
    private final PasswordEncoder passwordEncoder;
    private final IOtpService otpService;
    private final IUserService userService; // To reuse login

    @Override
    @Transactional
    public LoginResponseDTO registerRoger(RogerRegistrationDTO rogerRegistrationDTO) {
        logger.info("Registering new roger: email: {}", rogerRegistrationDTO.getEmail());

        if (userRepository.existsByEmail(rogerRegistrationDTO.getEmail())) {
            logger.warn("Roger registration failed - email already exists: {}", rogerRegistrationDTO.getEmail());
            throw new ConflictException("User", "A user with this email already exists");
        }

        if(!otpService.validateOtp(rogerRegistrationDTO.getEmail(), rogerRegistrationDTO.getOtp())) {
            logger.warn("Roger registration failed - invalid OTP: email: {}", rogerRegistrationDTO.getEmail());
            throw new com.heal.doctor.exception.BadRequestException("Invalid OTP");
        }

        String rogerId = generateRogerId();

        UserEntity user = UserEntity.builder()
                .userId(rogerId)
                .email(rogerRegistrationDTO.getEmail())
                .password(passwordEncoder.encode(rogerRegistrationDTO.getPassword()))
                .rolesEnum(RolesEnum.ROGER)
                .isActive(true)
                .emailVerified(true)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        userRepository.save(user);

        RogerEntity roger = RogerEntity.builder()
                .rogerId(rogerId)
                .firstName(rogerRegistrationDTO.getFirstName())
                .lastName(rogerRegistrationDTO.getLastName())
                .phoneNumber(rogerRegistrationDTO.getPhoneNumber())
                .profilePicture(rogerRegistrationDTO.getProfilePicture())
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        
        rogerRepository.save(roger);

        logger.info("Roger registered successfully: rogerId: {}, email: {}", rogerId, user.getEmail());
        
        return userService.login(rogerRegistrationDTO.getEmail(), rogerRegistrationDTO.getPassword());
    }

    @Override
    public RogerProfileDTO getRogerProfile() {
        String rogerId = CurrentUserName.getCurrentUserId();
        logger.debug("Fetching roger profile for rogerId: {}", rogerId);

        RogerEntity roger = rogerRepository.findByRogerId(rogerId)
                .orElseThrow(() -> new ResourceNotFoundException("Roger", rogerId));

        UserEntity user = userRepository.findByUserId(rogerId).orElse(null);
        String email = user != null ? user.getEmail() : null;

        return RogerProfileDTO.builder()
                .rogerId(roger.getRogerId())
                .firstName(roger.getFirstName())
                .lastName(roger.getLastName())
                .email(email)
                .phoneNumber(roger.getPhoneNumber())
                .profilePicture(roger.getProfilePicture())
                .address(roger.getAddress())
                .createdAt(roger.getCreatedAt())
                .updatedAt(roger.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public RogerProfileDTO updateRogerProfile(com.heal.doctor.dto.RogerUpdateDTO updateDTO) {
        String rogerId = CurrentUserName.getCurrentUserId();
        logger.info("Updating roger profile for rogerId: {}", rogerId);

        RogerEntity roger = rogerRepository.findByRogerId(rogerId)
                .orElseThrow(() -> new ResourceNotFoundException("Roger", rogerId));

        if (updateDTO.getFirstName() != null) roger.setFirstName(updateDTO.getFirstName());
        if (updateDTO.getLastName() != null) roger.setLastName(updateDTO.getLastName());
        if (updateDTO.getPhoneNumber() != null) roger.setPhoneNumber(updateDTO.getPhoneNumber());
        if (updateDTO.getProfilePicture() != null) roger.setProfilePicture(updateDTO.getProfilePicture());
        if (updateDTO.getAddress() != null) roger.setAddress(updateDTO.getAddress());

        roger.setUpdatedAt(new Date());
        rogerRepository.save(roger);

        return getRogerProfile();
    }

    private String generateRogerId() {
        String timestamp = new java.text.SimpleDateFormat("yyMMdd").format(new Date());
        String randomNumber = String.format("%04d", new java.util.Random().nextInt(10000));
        return "ROG-" + timestamp + "-" + randomNumber;
    }
}
