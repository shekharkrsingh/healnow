package com.heal.doctor.services.impl;

import com.heal.doctor.Mail.IDoctorAccountMailService;
import com.heal.doctor.Mail.impl.OtpServiceImpl;
import com.heal.doctor.dto.*;
import com.heal.doctor.models.DoctorEntity;
import com.heal.doctor.models.NotificationEntity;
import com.heal.doctor.models.enums.AvailableDayEnum;
import com.heal.doctor.models.enums.NotificationType;
import com.heal.doctor.repositories.DoctorRepository;
import com.heal.doctor.security.DoctorUserDetails;
import com.heal.doctor.security.JwtUtil;
import com.heal.doctor.services.IDoctorService;
import com.heal.doctor.services.INotificationService;
import com.heal.doctor.exception.BadRequestException;
import com.heal.doctor.exception.ConflictException;
import com.heal.doctor.exception.ResourceNotFoundException;
import com.heal.doctor.exception.UnauthorizedException;
import com.heal.doctor.exception.ValidationException;
import com.heal.doctor.utils.CurrentUserName;
import com.heal.doctor.utils.EmailValidatorUtil;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements IDoctorService {

    private static final Logger logger = LoggerFactory.getLogger(DoctorServiceImpl.class);
    private static final String DOCTOR_ID_PREFIX = "DOC";
    private static final String DOCTOR_ID_DATE_FORMAT = "yyMMdd-HHmm";
    private static final String DOCTOR_ID_RANDOM_FORMAT = "%03d";
    private static final int DOCTOR_ID_RANDOM_RANGE = 1000;
    private static final int VALID_PHONE_LENGTH = 10;
    private static final String PHONE_PATTERN = "^\\d{10}$";

    private final DoctorRepository doctorRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final OtpServiceImpl otpService;
    private final INotificationService notificationService;
    private final IDoctorAccountMailService doctorAccountMailService;

    @Transactional
    @Override
    public DoctorDTO createDoctor(DoctorRegistrationDTO doctorRegistrationDTO) {
        logger.info("Creating doctor account: email: {}, firstName: {}", 
                doctorRegistrationDTO.getEmail(), doctorRegistrationDTO.getFirstName());

        if (!EmailValidatorUtil.isValidEmail(doctorRegistrationDTO.getEmail())) {
            logger.warn("Doctor registration failed: Invalid email format: {}", doctorRegistrationDTO.getEmail());
            throw new ValidationException("Invalid email format");
        }
        if (doctorRepository.findByEmail(doctorRegistrationDTO.getEmail()).isPresent()) {
            logger.warn("Doctor registration failed: Email already exists: {}", doctorRegistrationDTO.getEmail());
            throw new ConflictException("Doctor", "A doctor with this email already exists");
        }
        if(doctorRegistrationDTO.getFirstName()==null || doctorRegistrationDTO.getPassword()==null){
            logger.warn("Doctor registration failed: Missing required fields - email: {}", doctorRegistrationDTO.getEmail());
            throw new ValidationException("First name and password are required");
        }
        if(doctorRegistrationDTO.getOtp()==null){
            logger.warn("Doctor registration failed: OTP missing - email: {}", doctorRegistrationDTO.getEmail());
            throw new ValidationException("OTP is required");
        }

        otpService.validateOtp(doctorRegistrationDTO.getEmail(), doctorRegistrationDTO.getOtp());
        logger.debug("OTP validated successfully for email: {}", doctorRegistrationDTO.getEmail());

        DoctorEntity doctor = modelMapper.map(doctorRegistrationDTO, DoctorEntity.class);
        doctor.setPassword(passwordEncoder.encode(doctorRegistrationDTO.getPassword()));
        doctor.setCreatedAt(new Date());
        doctor.setUpdatedAt(new Date());
        doctor.setDoctorId(generateDoctorId());
        DoctorEntity savedDoctor = doctorRepository.save(doctor);
        logger.info("Doctor account created successfully: doctorId: {}, email: {}, firstName: {}", 
                savedDoctor.getDoctorId(), savedDoctor.getEmail(), savedDoctor.getFirstName());
        NotificationEntity notification=NotificationEntity.builder().
                doctorId(savedDoctor.getDoctorId()).
                type(NotificationType.SYSTEM).
                title("Welcome "+ savedDoctor.getFirstName()).
                message("Your account has been successfully created. Complete your profile to start managing appointments and providing care.").
                build();
        notificationService.createNotificationAsync(notification).exceptionally(ex -> {
            logger.error("Failed to create welcome notification asynchronously: doctorId: {}, error: {}", 
                    savedDoctor.getDoctorId(), ex.getMessage(), ex);
            return null;
        });
        doctorAccountMailService.doctorWelcomeMail(savedDoctor.getFirstName(), savedDoctor.getEmail());
        
        return modelMapper.map(savedDoctor, DoctorDTO.class);
    }


    public String loginDoctor(String username, String password) {
        logger.info("Login attempt: username: {}", username);
        try {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(username, password);

            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            DoctorUserDetails userDetails = (DoctorUserDetails) userDetailsService.loadUserByUsername(username);

            String token = jwtUtil.generateToken(userDetails.getUsername(), userDetails.getDoctorId());
            logger.info("Login successful: username: {}, doctorId: {}", username, userDetails.getDoctorId());
            return token;
        } catch (Exception e) {
            logger.warn("Login failed: username: {}, error: {}", username, e.getMessage());
            throw e;
        }
    }


    @Override
    public DoctorDTO getDoctorById(String doctorId) {
        logger.debug("Fetching doctor by ID: doctorId: {}", doctorId);
        DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));
        logger.debug("Doctor retrieved: doctorId: {}, email: {}", doctorId, doctor.getEmail());
        return modelMapper.map(doctor, DoctorDTO.class);
    }

    @Override
    public DoctorDTO getDoctorProfile(){
        String username = CurrentUserName.getCurrentUsername();
        logger.debug("Fetching doctor profile: email: {}", username);
        DoctorEntity doctor = doctorRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile", username));
        logger.debug("Doctor profile retrieved: doctorId: {}, email: {}", doctor.getDoctorId(), username);
        return modelMapper.map(doctor, DoctorDTO.class);
    }

    @Override
    public List<DoctorDTO> getAllDoctors() {
        logger.debug("Fetching all doctors");
        List<DoctorDTO> doctors = doctorRepository.findAll().stream()
                .map(doctor -> modelMapper.map(doctor, DoctorDTO.class))
                .collect(Collectors.toList());
        logger.debug("Retrieved {} doctors", doctors.size());
        return doctors;
    }

    @Override
    public DoctorDTO updateDoctor(UpdateDoctorDetailsDTO updateDoctorDetailsDTO) {
        String username = CurrentUserName.getCurrentUsername();
        logger.info("Updating doctor profile: email: {}", username);
        DoctorEntity existingDoctor = doctorRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", username));

        if (updateDoctorDetailsDTO.getFirstName() != null && !updateDoctorDetailsDTO.getFirstName().isEmpty()) {
            existingDoctor.setFirstName(updateDoctorDetailsDTO.getFirstName());
        }
        if (updateDoctorDetailsDTO.getLastName() != null && !updateDoctorDetailsDTO.getLastName().isEmpty()) {
            existingDoctor.setLastName(updateDoctorDetailsDTO.getLastName());
        }
        if (updateDoctorDetailsDTO.getSpecialization() != null && !updateDoctorDetailsDTO.getSpecialization().isEmpty()) {
            existingDoctor.setSpecialization(updateDoctorDetailsDTO.getSpecialization());
        }
        if (updateDoctorDetailsDTO.getPhoneNumber() != null && !updateDoctorDetailsDTO.getPhoneNumber().isEmpty()) {
            String phoneNumber = updateDoctorDetailsDTO.getPhoneNumber().trim();
            if (phoneNumber.length() != VALID_PHONE_LENGTH) {
                logger.warn("Phone number update failed - invalid length: email: {}, phoneNumber length: {}", username, phoneNumber.length());
                throw new ValidationException("Phone number must be exactly " + VALID_PHONE_LENGTH + " digits.");
            }
            if (!phoneNumber.matches(PHONE_PATTERN)) {
                logger.warn("Phone number update failed - invalid format: email: {}", username);
                throw new ValidationException("Phone number must contain exactly " + VALID_PHONE_LENGTH + " digits.");
            }
            existingDoctor.setPhoneNumber(phoneNumber);
        }
        if (updateDoctorDetailsDTO.getAvailableDays() != null) {
            validateAvailableDays(updateDoctorDetailsDTO.getAvailableDays());
            existingDoctor.setAvailableDays(updateDoctorDetailsDTO.getAvailableDays());
        }
        if (updateDoctorDetailsDTO.getAvailableTimeSlots() != null) {
                existingDoctor.setAvailableTimeSlots(updateDoctorDetailsDTO.getAvailableTimeSlots());
        }
        if (updateDoctorDetailsDTO.getClinicAddress() != null && !updateDoctorDetailsDTO.getClinicAddress().isEmpty()) {
            existingDoctor.setClinicAddress(updateDoctorDetailsDTO.getClinicAddress());
        }
        if (updateDoctorDetailsDTO.getAddress() != null) {
            existingDoctor.setAddress(updateDoctorDetailsDTO.getAddress());
        }
        if (updateDoctorDetailsDTO.getEducation() != null) {
            existingDoctor.setEducation(updateDoctorDetailsDTO.getEducation());
        }
        if (updateDoctorDetailsDTO.getAchievementsAndAwards() != null) {
            existingDoctor.setAchievementsAndAwards(updateDoctorDetailsDTO.getAchievementsAndAwards());
        }
        if (updateDoctorDetailsDTO.getAbout() != null && !updateDoctorDetailsDTO.getAbout().isEmpty()) {
            existingDoctor.setAbout(updateDoctorDetailsDTO.getAbout());
        }
        if (updateDoctorDetailsDTO.getBio() != null && !updateDoctorDetailsDTO.getBio().isEmpty()) {
            existingDoctor.setBio(updateDoctorDetailsDTO.getBio());
        }
        if (updateDoctorDetailsDTO.getYearsOfExperience() != null) {
            existingDoctor.setYearsOfExperience(updateDoctorDetailsDTO.getYearsOfExperience());
        }
        if (updateDoctorDetailsDTO.getGender() != null) {
            existingDoctor.setGender(updateDoctorDetailsDTO.getGender());
        }
        if (updateDoctorDetailsDTO.getCoverPicture() != null && !updateDoctorDetailsDTO.getCoverPicture().isEmpty()) {
            existingDoctor.setCoverPicture(updateDoctorDetailsDTO.getCoverPicture());
        }
        if (updateDoctorDetailsDTO.getProfilePicture() != null && !updateDoctorDetailsDTO.getProfilePicture().isEmpty()) {
            existingDoctor.setProfilePicture(updateDoctorDetailsDTO.getProfilePicture());
        }

        existingDoctor.setUpdatedAt(new Date());

        DoctorEntity updatedDoctor = doctorRepository.save(existingDoctor);

        DoctorDTO doctorDTO = new DoctorDTO();
        modelMapper.map(updatedDoctor, doctorDTO);

        return doctorDTO;
    }





    @Override
    public void deleteDoctor(String doctorId) {
        logger.warn("Deleting doctor account: doctorId: {}", doctorId);
        DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));
        String email = doctor.getEmail();
        doctorRepository.delete(doctor);
        logger.warn("Doctor account deleted: doctorId: {}, email: {}", doctorId, email);
    }


    @Override
    public void changePassword(ChangePasswordDTO changePasswordDTO) {
        String username = CurrentUserName.getCurrentUsername();
        logger.info("Changing password: email: {}", username);
        DoctorEntity doctor = doctorRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", username));

        if (!passwordEncoder.matches(changePasswordDTO.getOldPassword(), doctor.getPassword())) {
            logger.warn("Password change failed - invalid old password: email: {}", username);
            throw new UnauthorizedException("Invalid old password");
        }
        doctor.setUpdatedAt(new Date());
        doctor.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
        DoctorEntity savedDoctor= doctorRepository.save(doctor);
        logger.info("Password changed successfully: doctorId: {}, email: {}", savedDoctor.getDoctorId(), username);
        NotificationEntity notification=NotificationEntity.builder()
                .doctorId(savedDoctor.getDoctorId())
                .type(NotificationType.INFO)
                .title("Password Updated.")
                .message("Your login credentials have been updated.")
                .build();
        notificationService.createNotificationAsync(notification).exceptionally(ex -> {
            logger.error("Failed to create password change notification asynchronously: doctorId: {}, error: {}", 
                    savedDoctor.getDoctorId(), ex.getMessage(), ex);
            return null;
        });
        doctorAccountMailService.doctorPasswordChangeMail(savedDoctor.getFirstName(), savedDoctor.getEmail());
    }

    @Override
    public String updateEmail(UpdateEmailDTO updateEmailDTO) {
        String username = CurrentUserName.getCurrentUsername();
        logger.info("Updating email: oldEmail: {}, newEmail: {}", username, updateEmailDTO.getNewEmail());
        DoctorEntity doctor = doctorRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", username));
        Optional<DoctorEntity> doctorIsAlreadyAvailable = doctorRepository.findByEmail(updateEmailDTO.getNewEmail());

        if (doctorIsAlreadyAvailable.isPresent()) {
            logger.warn("Email update failed - new email already exists: oldEmail: {}, newEmail: {}", 
                    username, updateEmailDTO.getNewEmail());
            throw new ConflictException("Email", "A doctor with email '" + updateEmailDTO.getNewEmail() + "' already exists");
        }

        if (!passwordEncoder.matches(updateEmailDTO.getPassword(), doctor.getPassword())) {
            logger.warn("Email update failed - invalid password: email: {}", username);
            throw new UnauthorizedException("Invalid password");
        }

        if (!otpService.validateOtp(updateEmailDTO.getNewEmail(), updateEmailDTO.getOtp())) {
            logger.warn("Email update failed - invalid OTP: newEmail: {}", updateEmailDTO.getNewEmail());
            throw new BadRequestException("Invalid OTP");
        }
        String oldMail=doctor.getEmail();
        doctor.setEmail(updateEmailDTO.getNewEmail());
        doctor.setUpdatedAt(new Date());
        DoctorEntity savedDoctor =doctorRepository.save(doctor);
        logger.info("Email updated successfully: doctorId: {}, oldEmail: {}, newEmail: {}", 
                savedDoctor.getDoctorId(), oldMail, updateEmailDTO.getNewEmail());
        NotificationEntity notification=NotificationEntity.builder()
                .doctorId(savedDoctor.getDoctorId())
                .type(NotificationType.INFO)
                .title("Security Update")
                .message("Your login email has been changed. If this wasn’t you, please review your security settings.")
                .build();
        notificationService.createNotificationAsync(notification).exceptionally(ex -> {
            logger.error("Failed to create email change notification asynchronously: doctorId: {}, error: {}", 
                    savedDoctor.getDoctorId(), ex.getMessage(), ex);
            return null;
        });
        doctorAccountMailService.doctorLoginEmailChangedMail(
                oldMail,
                doctor.getFirstName(),
                oldMail,
                updateEmailDTO.getNewEmail()
        );
        doctorAccountMailService.doctorLoginEmailChangedMail(
                updateEmailDTO.getNewEmail(),
                doctor.getFirstName(),
                oldMail,
                updateEmailDTO.getNewEmail()
        );
        return loginDoctor(updateEmailDTO.getNewEmail(), updateEmailDTO.getPassword());
    }

    @Override
    public void forgotPassword(ForgotPasswordDTO forgotPasswordDTO) {
        logger.info("Password reset request: email: {}", forgotPasswordDTO.getEmail());
        DoctorEntity doctor = doctorRepository.findByEmail(forgotPasswordDTO.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", forgotPasswordDTO.getEmail()));

        if (!otpService.validateOtp(forgotPasswordDTO.getEmail(), forgotPasswordDTO.getOtp())) {
            logger.warn("Password reset failed - invalid OTP: email: {}", forgotPasswordDTO.getEmail());
            throw new BadRequestException("Invalid OTP");
        }

        doctor.setPassword(passwordEncoder.encode(forgotPasswordDTO.getNewPassword()));
        doctor.setUpdatedAt(new Date());
        DoctorEntity savedDoctor= doctorRepository.save(doctor);
        logger.info("Password reset successfully: doctorId: {}, email: {}", savedDoctor.getDoctorId(), forgotPasswordDTO.getEmail());
        NotificationEntity notification=NotificationEntity.builder()
                .doctorId(savedDoctor.getDoctorId())
                .type(NotificationType.INFO)
                .title("Password Updated.")
                .message("Your login credentials have been updated.")
                .build();
        notificationService.createNotificationAsync(notification).exceptionally(ex -> {
            logger.error("Failed to create password reset notification asynchronously: doctorId: {}, error: {}", 
                    savedDoctor.getDoctorId(), ex.getMessage(), ex);
            return null;
        });
        doctorAccountMailService.doctorPasswordChangeMail(
                savedDoctor.getFirstName(),
                savedDoctor.getEmail()
            );
    }


    private String generateDoctorId() {
        String prefix = DOCTOR_ID_PREFIX;
        String timestamp = new SimpleDateFormat(DOCTOR_ID_DATE_FORMAT).format(new Date());
        String randomNumber = String.format(DOCTOR_ID_RANDOM_FORMAT, new Random().nextInt(DOCTOR_ID_RANDOM_RANGE));
        return String.format("%s-%s-%s", prefix, timestamp, randomNumber);
    }

    private void validateAvailableDays(List<AvailableDayEnum> availableDays) {
        for (AvailableDayEnum day : availableDays) {
            if (day == null) {
                throw new ValidationException("Invalid day in available days");
            }
        }
    }

}

