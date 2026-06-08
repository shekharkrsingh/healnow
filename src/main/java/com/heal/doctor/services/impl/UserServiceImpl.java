package com.heal.doctor.services.impl;


import com.heal.doctor.Mail.IUserAccountEmailService;
import com.heal.doctor.Mail.impl.OtpServiceImpl;
import com.heal.doctor.dto.ChangePasswordDTO;
import com.heal.doctor.dto.ForgotPasswordDTO;
import com.heal.doctor.dto.UpdateEmailDTO;
import com.heal.doctor.models.RogerEntity;
import com.heal.doctor.repositories.RogerRepository;
import com.heal.doctor.exception.*;
import com.heal.doctor.models.CollaboratorProfileEntity;
import com.heal.doctor.models.DoctorEntity;
import com.heal.doctor.models.NotificationEntity;
import com.heal.doctor.models.UserEntity;
import com.heal.doctor.models.enums.NotificationRecipientType;
import com.heal.doctor.models.enums.NotificationType;
import com.heal.doctor.models.enums.RolesEnum;
import com.heal.doctor.repositories.CollaboratorProfileRepository;
import com.heal.doctor.repositories.DoctorRepository;
import com.heal.doctor.repositories.UserRepository;
import com.heal.doctor.security.CollaboratorUserDetails;
import com.heal.doctor.security.DoctorUserDetails;
import com.heal.doctor.security.JwtUtil;
import com.heal.doctor.services.INotificationService;
import com.heal.doctor.services.IUserService;
import com.heal.doctor.utils.CurrentUserName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class UserServiceImpl implements IUserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private static final int MIN_IMAGE_WIDTH = 200;
    private static final int MIN_IMAGE_HEIGHT = 200;
    private static final int MAX_IMAGE_WIDTH = 4096;
    private static final int MAX_IMAGE_HEIGHT = 4096;

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final CollaboratorProfileRepository collaboratorProfileRepository;
    private final RogerRepository rogerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final OtpServiceImpl otpService;
    private final INotificationService notificationService;
    private final IUserAccountEmailService userAccountEmailService;
    private final Executor taskExecutor;

    public UserServiceImpl(
            DoctorRepository doctorRepository,
            UserRepository userRepository,
            CollaboratorProfileRepository collaboratorProfileRepository,
            RogerRepository rogerRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            OtpServiceImpl otpService,
            INotificationService notificationService,
            IUserAccountEmailService userAccountEmailService,
            @Qualifier("notificationTaskExecutor") Executor taskExecutor) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.collaboratorProfileRepository = collaboratorProfileRepository;
        this.rogerRepository = rogerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.otpService = otpService;
        this.notificationService = notificationService;
        this.userAccountEmailService = userAccountEmailService;
        this.taskExecutor = taskExecutor;
    }


    @Override
    @Transactional
    public String login(String username, String password) {
        logger.info("Login attempt: username: {}", username);
        try {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(username, password);

            authenticationManager.authenticate(authenticationToken);

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            String userId;
            String doctorId;
            String role;

            if (userDetails instanceof DoctorUserDetails doctorUserDetails) {
                userId = doctorUserDetails.getDoctorId();
                role = doctorUserDetails.getUser().getRolesEnum() != null
                        ? doctorUserDetails.getUser().getRolesEnum().name()
                        : "DOCTOR";
                // Only set doctorId if the role is actually DOCTOR
                doctorId = "DOCTOR".equals(role) ? userId : null;
            } else if (userDetails instanceof CollaboratorUserDetails collaboratorUserDetails) {
                userId = collaboratorUserDetails.getUserId();
                doctorId = collaboratorUserDetails.getDoctorId();
                role = collaboratorUserDetails.getUser().getRolesEnum() != null
                        ? collaboratorUserDetails.getUser().getRolesEnum().name()
                        : "COLLABORATOR";
            } else {
                userId = userDetails.getUsername();
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
                .targetId(CurrentUserName.getCurrentUserId())
                .recipientType(NotificationRecipientType.INDIVIDUAL)
                .type(NotificationType.INFO)
                .title("Password Updated.")
                .message("Your login credentials have been updated.")
                .build();
        notificationService.createNotificationAsync(notification).exceptionally(ex -> {
            logger.error("Failed to create password change notification asynchronously: doctorId: {}, error: {}",
                    doctorId, ex.getMessage(), ex);
            return null;
        });
        doctorRepository.findByDoctorId(doctorId)
                .ifPresent(doctor -> userAccountEmailService.passwordChangeMail(
                        doctor.getFirstName(),
                        username
                ));
    }

    @Override
    @Transactional
    public String updateEmail(UpdateEmailDTO updateEmailDTO) {
        String username = CurrentUserName.getCurrentUsername();
        String userId = CurrentUserName.getCurrentUserId();
        logger.info("Updating email: oldEmail: {}, newEmail: {}, userId: {}", username, updateEmailDTO.getNewEmail(), userId);
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
                .targetId(CurrentUserName.getCurrentUserId())
                .recipientType(NotificationRecipientType.INDIVIDUAL)
                .type(NotificationType.INFO)
                .title("Security Update")
                .message("Your login email has been changed. If this wasn’t you, please review your security settings.")
                .build();
        notificationService.createNotificationAsync(notification).exceptionally(ex -> {
            logger.error("Failed to create email change notification asynchronously: userId: {}, error: {}",
                    userId, ex.getMessage(), ex);
            return null;
        });

        DoctorEntity doctor = doctorRepository.findByDoctorId(userId).orElse(null);
        String firstName = "User";
        if (doctor != null) {
            firstName = doctor.getFirstName();
        } else {
             // Try fetching Roger profile if not a doctor
             RogerEntity roger = rogerRepository.findByRogerId(userId).orElse(null);
             if (roger != null) {
                 firstName = roger.getFirstName();
             }
        }

        String finalFirstName = firstName;
        CompletableFuture<Void> oldEmailFuture = CompletableFuture.runAsync(() ->
                        userAccountEmailService.loginEmailChangedMail(
                                oldMail, finalFirstName, oldMail, updateEmailDTO.getNewEmail()),
                taskExecutor);

        CompletableFuture<Void> newEmailFuture = CompletableFuture.runAsync(() ->
                        userAccountEmailService.loginEmailChangedMail(
                                updateEmailDTO.getNewEmail(), finalFirstName, oldMail, updateEmailDTO.getNewEmail()),
                taskExecutor);

        CompletableFuture.allOf(oldEmailFuture, newEmailFuture).exceptionally(ex -> {
            logger.error("Failed to send email change notifications in parallel: error: {}", ex.getMessage(), ex);
            return null;
        });

        return login(updateEmailDTO.getNewEmail(), updateEmailDTO.getPassword());
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
                .targetId(savedUser.getUserId())
                .recipientType(NotificationRecipientType.INDIVIDUAL)
                .type(NotificationType.INFO)
                .title("Password Updated.")
                .message("Your login credentials have been updated.")
                .build();
        notificationService.createNotificationAsync(notification).exceptionally(ex -> {
            logger.error("Failed to create password reset notification asynchronously: doctorId: {}, error: {}",
                    savedUser.getUserId(), ex.getMessage(), ex);
            return null;
        });
        if(savedUser.getRolesEnum().equals(RolesEnum.DOCTOR))
        doctorRepository.findByDoctorId(savedUser.getUserId())
                .ifPresent(doctor -> userAccountEmailService.passwordChangeMail(
                        doctor.getFirstName(), savedUser.getEmail()
                ));
        else if(savedUser.getRolesEnum().equals(RolesEnum.COLLABORATOR)){
            collaboratorProfileRepository.findByCollaboratorId(savedUser.getUserId())
                    .ifPresent(collaborator -> userAccountEmailService.passwordChangeMail(
                            collaborator.getFirstName(), savedUser.getEmail()
                    ));
        }
    }


    @Transactional
    @Override
    public String changeProfilePicture(MultipartFile file) {
        String userId = CurrentUserName.getCurrentUserId();
        logger.info("Changing profile picture: userId: {}", userId);

        validateImageFile(file);

        String imageUrl = savePictureToCloud(file);
        
        String role = CurrentUserName.getCurrentUserRole();
        if ("ROGER".equalsIgnoreCase(role)) {
             RogerEntity roger = rogerRepository.findByRogerId(userId)
                     .orElseThrow(() -> new ResourceNotFoundException("Roger", userId));
             roger.setProfilePicture(imageUrl);
             roger.setUpdatedAt(new Date());
             rogerRepository.save(roger);
        } else if ("COLLABORATOR".equalsIgnoreCase(role)) {
             CollaboratorProfileEntity collaboratorProfile = collaboratorProfileRepository.findByCollaboratorId(userId)
                     .orElseThrow(() -> new ResourceNotFoundException("Collaborator profile", userId));
             collaboratorProfile.setProfilePicture(imageUrl);
             collaboratorProfile.setUpdatedAt(new Date());
             collaboratorProfileRepository.save(collaboratorProfile);
        } else {
             // Default to Doctor/Admin behavior
             DoctorEntity doctor = doctorRepository.findByDoctorId(userId)
                     .orElseThrow(() -> new ResourceNotFoundException("Doctor", userId));

             doctor.setProfilePicture(imageUrl);
             doctor.setUpdatedAt(new Date());
             doctorRepository.save(doctor);
        }

        logger.info("Profile picture updated successfully: userId: {}", userId);
        return imageUrl;
    }

    @Transactional
    @Override
    public String changeCoverPicture(MultipartFile file) {
        String userId = CurrentUserName.getCurrentUserId();
        logger.info("Changing cover picture: userId: {}", userId);

        validateImageFile(file);

        String role = CurrentUserName.getCurrentUserRole();
        // Check if user is ROGER, they don't have cover picture yet
        if ("ROGER".equalsIgnoreCase(role)) {
            throw new ForbiddenException("Roger users cannot update cover picture yet");
        }

        String imageUrl = savePictureToCloud(file);
        
        if ("COLLABORATOR".equalsIgnoreCase(role)) {
             CollaboratorProfileEntity collaboratorProfile = collaboratorProfileRepository.findByCollaboratorId(userId)
                     .orElseThrow(() -> new ResourceNotFoundException("Collaborator profile", userId));
             collaboratorProfile.setCoverPicture(imageUrl);
             collaboratorProfile.setUpdatedAt(new Date());
             collaboratorProfileRepository.save(collaboratorProfile);
        } else {
             DoctorEntity doctor = doctorRepository.findByDoctorId(userId)
                     .orElseThrow(() -> new ResourceNotFoundException("Doctor", userId));

             doctor.setCoverPicture(imageUrl);
             doctor.setUpdatedAt(new Date());
             doctorRepository.save(doctor);
        }

        logger.info("Cover picture updated successfully: userId: {}", userId);
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
}
