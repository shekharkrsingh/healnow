package com.heal.doctor.services.impl;

import com.heal.doctor.Mail.IDoctorAccountMailService;
import com.heal.doctor.Mail.impl.OtpServiceImpl;
import com.heal.doctor.dto.*;
import com.heal.doctor.models.DoctorEntity;
import com.heal.doctor.models.NotificationEntity;
import com.heal.doctor.models.enums.AvailableDayEnum;
import com.heal.doctor.models.enums.NotificationType;
import com.heal.doctor.models.enums.RolesEnum;
import com.heal.doctor.models.UserEntity;
import com.heal.doctor.repositories.DoctorRepository;
import com.heal.doctor.repositories.UserRepository;
import com.heal.doctor.security.CollaboratorUserDetails;
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
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class DoctorServiceImpl implements IDoctorService {

    private static final Logger logger = LoggerFactory.getLogger(DoctorServiceImpl.class);
    private static final String DOCTOR_ID_PREFIX = "DOC";
    private static final String DOCTOR_ID_DATE_FORMAT = "yyMMdd-HHmm";
    private static final String DOCTOR_ID_RANDOM_FORMAT = "%03d";
    private static final int DOCTOR_ID_RANDOM_RANGE = 1000;
    private static final int VALID_PHONE_LENGTH = 10;
    private static final String PHONE_PATTERN = "^\\d{10}$";
    private static final int MIN_IMAGE_WIDTH = 200;
    private static final int MIN_IMAGE_HEIGHT = 200;
    private static final int MAX_IMAGE_WIDTH = 4096;
    private static final int MAX_IMAGE_HEIGHT = 4096;

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final OtpServiceImpl otpService;
    private final INotificationService notificationService;
    private final IDoctorAccountMailService doctorAccountMailService;
    private final Executor taskExecutor;

    public DoctorServiceImpl(DoctorRepository doctorRepository, UserRepository userRepository,
                            ModelMapper modelMapper,
                            PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                            AuthenticationManager authenticationManager,
                            UserDetailsService userDetailsService, OtpServiceImpl otpService,
                            INotificationService notificationService,
                            IDoctorAccountMailService doctorAccountMailService,
                            @Qualifier("emailTaskExecutor") Executor taskExecutor) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.otpService = otpService;
        this.notificationService = notificationService;
        this.doctorAccountMailService = doctorAccountMailService;
        this.taskExecutor = taskExecutor;
    }

    @Transactional
    @Override
    public DoctorDTO createDoctor(DoctorRegistrationDTO doctorRegistrationDTO) {
        logger.info("Creating doctor account: email: {}, firstName: {}", 
                doctorRegistrationDTO.getEmail(), doctorRegistrationDTO.getFirstName());

        if (!EmailValidatorUtil.isValidEmail(doctorRegistrationDTO.getEmail())) {
            logger.warn("Doctor registration failed: Invalid email format: {}", doctorRegistrationDTO.getEmail());
            throw new ValidationException("Invalid email format");
        }
        if (userRepository.existsByEmail(doctorRegistrationDTO.getEmail())) {
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

        // Generate doctor ID (this will be used as userId)
        String doctorId = generateDoctorId();

        // Create UserEntity for authentication
        UserEntity user = UserEntity.builder()
                .userId(doctorId)
                .email(doctorRegistrationDTO.getEmail())
                .password(passwordEncoder.encode(doctorRegistrationDTO.getPassword()))
                .rolesEnum(RolesEnum.DOCTOR)
                .isActive(true)
                .emailVerified(false)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        UserEntity savedUser = userRepository.save(user);
        logger.info("User created for doctor: userId: {}, email: {}", doctorId, doctorRegistrationDTO.getEmail());

        // Create DoctorEntity profile
        DoctorEntity doctor = modelMapper.map(doctorRegistrationDTO, DoctorEntity.class);
        doctor.setDoctorId(doctorId); // Link to UserEntity.userId
        doctor.setCreatedAt(new Date());
        doctor.setUpdatedAt(new Date());
        DoctorEntity savedDoctor = doctorRepository.save(doctor);
        logger.info("Doctor account created successfully: doctorId: {}, email: {}, firstName: {}", 
                savedDoctor.getDoctorId(), savedUser.getEmail(), savedDoctor.getFirstName());
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
        doctorAccountMailService.doctorWelcomeMail(savedDoctor.getFirstName(), savedUser.getEmail());
        
        return modelMapper.map(savedDoctor, DoctorDTO.class);
    }


    public String loginDoctor(String username, String password) {
        logger.info("Login attempt: username: {}", username);
        try {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(username, password);

            authenticationManager.authenticate(authenticationToken);

            org.springframework.security.core.userdetails.UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            String userId;
            String doctorId;
            String role;

            if (userDetails instanceof DoctorUserDetails) {
                DoctorUserDetails doctorUserDetails = (DoctorUserDetails) userDetails;
                userId = doctorUserDetails.getDoctorId();
                doctorId = doctorUserDetails.getDoctorId(); // For doctors, userId = doctorId
                role = doctorUserDetails.getUser().getRolesEnum() != null 
                        ? doctorUserDetails.getUser().getRolesEnum().name() 
                        : "DOCTOR";
            } else if (userDetails instanceof CollaboratorUserDetails) {
                CollaboratorUserDetails collaboratorUserDetails = (CollaboratorUserDetails) userDetails;
                userId = collaboratorUserDetails.getUserId();
                doctorId = collaboratorUserDetails.getDoctorId(); // For collaborators, doctorId is the associated doctor
                role = collaboratorUserDetails.getUser().getRolesEnum() != null 
                        ? collaboratorUserDetails.getUser().getRolesEnum().name() 
                        : "COLLABORATOR";
            } else {
                // Fallback for other roles (ADMIN, USER)
                userId = userDetails.getUsername(); // Use email as fallback
                doctorId = null;
                role = "USER";
            }

            String token = jwtUtil.generateToken(userDetails.getUsername(), userId, doctorId, role);
            logger.info("Login successful: username: {}, userId: {}, doctorId: {}, role: {}", username, userId, doctorId, role);
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
        UserEntity user = userRepository.findByUserId(doctorId)
                .orElse(null);
        logger.debug("Doctor retrieved: doctorId: {}, email: {}", doctorId, user != null ? user.getEmail() : "N/A");
        DoctorDTO doctorDTO = modelMapper.map(doctor, DoctorDTO.class);
        if (user != null) {
            doctorDTO.setEmail(user.getEmail());
        }
        return doctorDTO;
    }

    @Override
    public DoctorDTO getDoctorProfile(){
        String username = CurrentUserName.getCurrentUsername();
        String doctorId = CurrentUserName.getCurrentDoctorId();
        logger.debug("Fetching doctor profile: email: {}, doctorId: {}", username, doctorId);
        DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile", doctorId));
        UserEntity user = userRepository.findByEmail(username)
                .orElse(null);
        logger.debug("Doctor profile retrieved: doctorId: {}, email: {}", doctor.getDoctorId(), username);
        DoctorDTO doctorDTO = modelMapper.map(doctor, DoctorDTO.class);
        if (user != null) {
            doctorDTO.setEmail(user.getEmail());
        }
        return doctorDTO;
    }

    @Override
    public List<DoctorDTO> getAllDoctors() {
        logger.debug("Fetching all doctors");
        List<DoctorDTO> doctors = doctorRepository.findAll().parallelStream()
                .map(doctor -> modelMapper.map(doctor, DoctorDTO.class))
                .collect(Collectors.toList());
        logger.debug("Retrieved {} doctors", doctors.size());
        return doctors;
    }

    @Transactional
    @Override
    public DoctorDTO updateDoctor(UpdateDoctorDetailsDTO updateDoctorDetailsDTO) {
        String username = CurrentUserName.getCurrentUsername();
        String doctorId = CurrentUserName.getCurrentDoctorId();
        logger.info("Updating doctor profile: email: {}, doctorId: {}", username, doctorId);
        DoctorEntity existingDoctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));

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
        
        // Add email from UserEntity
        UserEntity user = userRepository.findByUserId(doctorId).orElse(null);
        if (user != null) {
            doctorDTO.setEmail(user.getEmail());
        }

        return doctorDTO;
    }





    @Override
    @Transactional
    public void deleteDoctor(String doctorId) {
        logger.warn("Deleting doctor account: doctorId: {}", doctorId);
        DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));
        UserEntity user = userRepository.findByUserId(doctorId)
                .orElse(null);
        String email = user != null ? user.getEmail() : "N/A";
        doctorRepository.delete(doctor);
        if (user != null) {
            userRepository.delete(user);
        }
        logger.warn("Doctor account deleted: doctorId: {}, email: {}", doctorId, email);
    }


    @Override
    @Transactional
    public void changePassword(ChangePasswordDTO changePasswordDTO) {
        String username = CurrentUserName.getCurrentUsername();
        String doctorId = CurrentUserName.getCurrentDoctorId();
        logger.info("Changing password: email: {}, doctorId: {}", username, doctorId);
        UserEntity user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        if (!passwordEncoder.matches(changePasswordDTO.getOldPassword(), user.getPassword())) {
            logger.warn("Password change failed - invalid old password: email: {}", username);
            throw new UnauthorizedException("Invalid old password");
        }
        user.setUpdatedAt(new Date());
        user.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
        UserEntity savedUser = userRepository.save(user);
        logger.info("Password changed successfully: userId: {}, email: {}", savedUser.getUserId(), username);
        NotificationEntity notification=NotificationEntity.builder()
                .doctorId(doctorId)
                .type(NotificationType.INFO)
                .title("Password Updated.")
                .message("Your login credentials have been updated.")
                .build();
        notificationService.createNotificationAsync(notification).exceptionally(ex -> {
            logger.error("Failed to create password change notification asynchronously: doctorId: {}, error: {}", 
                    doctorId, ex.getMessage(), ex);
            return null;
        });
        DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId).orElse(null);
        if (doctor != null) {
            doctorAccountMailService.doctorPasswordChangeMail(doctor.getFirstName(), username);
        }
    }

    @Override
    @Transactional
    public String updateEmail(UpdateEmailDTO updateEmailDTO) {
        String username = CurrentUserName.getCurrentUsername();
        String doctorId = CurrentUserName.getCurrentDoctorId();
        logger.info("Updating email: oldEmail: {}, newEmail: {}, doctorId: {}", username, updateEmailDTO.getNewEmail(), doctorId);
        UserEntity user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        if (userRepository.existsByEmail(updateEmailDTO.getNewEmail())) {
            logger.warn("Email update failed - new email already exists: oldEmail: {}, newEmail: {}", 
                    username, updateEmailDTO.getNewEmail());
            throw new ConflictException("Email", "A user with email '" + updateEmailDTO.getNewEmail() + "' already exists");
        }

        if (!passwordEncoder.matches(updateEmailDTO.getPassword(), user.getPassword())) {
            logger.warn("Email update failed - invalid password: email: {}", username);
            throw new UnauthorizedException("Invalid password");
        }

        if (!otpService.validateOtp(updateEmailDTO.getNewEmail(), updateEmailDTO.getOtp())) {
            logger.warn("Email update failed - invalid OTP: newEmail: {}", updateEmailDTO.getNewEmail());
            throw new BadRequestException("Invalid OTP");
        }
        String oldMail = user.getEmail();
        user.setEmail(updateEmailDTO.getNewEmail());
        user.setUpdatedAt(new Date());
        UserEntity savedUser = userRepository.save(user);
        logger.info("Email updated successfully: userId: {}, oldEmail: {}, newEmail: {}", 
                savedUser.getUserId(), oldMail, updateEmailDTO.getNewEmail());
        NotificationEntity notification=NotificationEntity.builder()
                .doctorId(doctorId)
                .type(NotificationType.INFO)
                .title("Security Update")
                .message("Your login email has been changed. If this wasn’t you, please review your security settings.")
                .build();
        notificationService.createNotificationAsync(notification).exceptionally(ex -> {
            logger.error("Failed to create email change notification asynchronously: doctorId: {}, error: {}", 
                    doctorId, ex.getMessage(), ex);
            return null;
        });
        
        DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId).orElse(null);
        String firstName = doctor != null ? doctor.getFirstName() : "User";
        
        CompletableFuture<Void> oldEmailFuture = CompletableFuture.runAsync(() ->
                doctorAccountMailService.doctorLoginEmailChangedMail(
                        oldMail, firstName, oldMail, updateEmailDTO.getNewEmail()),
                taskExecutor);
        
        CompletableFuture<Void> newEmailFuture = CompletableFuture.runAsync(() ->
                doctorAccountMailService.doctorLoginEmailChangedMail(
                        updateEmailDTO.getNewEmail(), firstName, oldMail, updateEmailDTO.getNewEmail()),
                taskExecutor);
        
        CompletableFuture.allOf(oldEmailFuture, newEmailFuture).exceptionally(ex -> {
            logger.error("Failed to send email change notifications in parallel: error: {}", ex.getMessage(), ex);
            return null;
        });
        
        return loginDoctor(updateEmailDTO.getNewEmail(), updateEmailDTO.getPassword());
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordDTO forgotPasswordDTO) {
        logger.info("Password reset request: email: {}", forgotPasswordDTO.getEmail());
        UserEntity user = userRepository.findByEmail(forgotPasswordDTO.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", forgotPasswordDTO.getEmail()));

        if (!otpService.validateOtp(forgotPasswordDTO.getEmail(), forgotPasswordDTO.getOtp())) {
            logger.warn("Password reset failed - invalid OTP: email: {}", forgotPasswordDTO.getEmail());
            throw new BadRequestException("Invalid OTP");
        }

        user.setPassword(passwordEncoder.encode(forgotPasswordDTO.getNewPassword()));
        user.setUpdatedAt(new Date());
        UserEntity savedUser = userRepository.save(user);
        logger.info("Password reset successfully: userId: {}, email: {}", savedUser.getUserId(), forgotPasswordDTO.getEmail());
        NotificationEntity notification=NotificationEntity.builder()
                .doctorId(savedUser.getUserId())
                .type(NotificationType.INFO)
                .title("Password Updated.")
                .message("Your login credentials have been updated.")
                .build();
        notificationService.createNotificationAsync(notification).exceptionally(ex -> {
            logger.error("Failed to create password reset notification asynchronously: doctorId: {}, error: {}", 
                    savedUser.getUserId(), ex.getMessage(), ex);
            return null;
        });
        DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId).orElse(null);
        if (doctor != null) {
            doctorAccountMailService.doctorPasswordChangeMail(doctor.getFirstName(), username);
        }
    }

    @Transactional
    @Override
    public String changeProfilePicture(MultipartFile file) {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        logger.info("Changing profile picture: doctorId: {}", doctorId);

        validateImageFile(file);

        String imageUrl = savePictureToCloud(file);
        DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));

        doctor.setProfilePicture(imageUrl);
        doctor.setUpdatedAt(new Date());
        doctorRepository.save(doctor);

        logger.info("Profile picture updated successfully: doctorId: {}", doctorId);
        return imageUrl;
    }

    @Transactional
    @Override
    public String changeCoverPicture(MultipartFile file) {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        logger.info("Changing cover picture: doctorId: {}", doctorId);

        validateImageFile(file);

        String imageUrl = savePictureToCloud(file);
        DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));

        doctor.setCoverPicture(imageUrl);
        doctor.setUpdatedAt(new Date());
        doctorRepository.save(doctor);

        logger.info("Cover picture updated successfully: doctorId: {}", doctorId);
        return imageUrl;
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            logger.warn("Image file validation failed: file is null or empty");
            throw new ValidationException("Image file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            logger.warn("Image file validation failed: invalid content type: {}", contentType);
            throw new ValidationException("File must be an image");
        }

        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            logger.warn("Image file validation failed: file size exceeds limit: {} bytes", file.getSize());
            throw new ValidationException("Image file size must not exceed 5MB");
        }

        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                logger.warn("Image file validation failed: unable to read image");
                throw new ValidationException("Invalid image file format");
            }

            int width = image.getWidth();
            int height = image.getHeight();

            if (width < MIN_IMAGE_WIDTH || height < MIN_IMAGE_HEIGHT) {
                logger.warn("Image file validation failed: resolution too low: {}x{}", width, height);
                throw new ValidationException(
                        String.format("Image resolution must be at least %dx%d pixels", MIN_IMAGE_WIDTH, MIN_IMAGE_HEIGHT)
                );
            }

            if (width > MAX_IMAGE_WIDTH || height > MAX_IMAGE_HEIGHT) {
                logger.warn("Image file validation failed: resolution too high: {}x{}", width, height);
                throw new ValidationException(
                        String.format("Image resolution must not exceed %dx%d pixels", MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT)
                );
            }

            logger.debug("Image file validation passed: {}x{}, size: {} bytes", width, height, file.getSize());
        } catch (IOException e) {
            logger.error("Image file validation failed: error reading image: {}", e.getMessage());
            throw new ValidationException("Failed to process image file");
        }
    }

    private String savePictureToCloud(MultipartFile file){
        return "www.google.com";
    }


    private String generateDoctorId() {
        String timestamp = new SimpleDateFormat(DOCTOR_ID_DATE_FORMAT).format(new Date());
        String randomNumber = String.format(DOCTOR_ID_RANDOM_FORMAT, new Random().nextInt(DOCTOR_ID_RANDOM_RANGE));
        return String.format("%s-%s-%s", DOCTOR_ID_PREFIX, timestamp, randomNumber);
    }

    private void validateAvailableDays(List<AvailableDayEnum> availableDays) {
        for (AvailableDayEnum day : availableDays) {
            if (day == null) {
                throw new ValidationException("Invalid day in available days");
            }
        }
    }

}

