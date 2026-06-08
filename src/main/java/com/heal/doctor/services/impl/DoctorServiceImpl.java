package com.heal.doctor.services.impl;

import com.heal.doctor.Mail.IDoctorAccountMailService;
import com.heal.doctor.Mail.impl.OtpServiceImpl;
import com.heal.doctor.dto.*;
import com.heal.doctor.models.DoctorEntity;
import com.heal.doctor.models.DoctorVerificationRequestEntity;
import com.heal.doctor.models.NotificationEntity;
import com.heal.doctor.models.enums.AvailableDayEnum;
import com.heal.doctor.models.enums.NotificationRecipientType;
import com.heal.doctor.models.enums.NotificationType;
import com.heal.doctor.models.enums.RequestStatus;
import com.heal.doctor.models.enums.RolesEnum;
import com.heal.doctor.models.enums.VerificationStatus;
import com.heal.doctor.models.UserEntity;
import com.heal.doctor.repositories.DoctorRepository;
import com.heal.doctor.repositories.DoctorVerificationRequestRepository;
import com.heal.doctor.repositories.UserRepository;
import com.heal.doctor.services.IDoctorService;
import com.heal.doctor.services.INotificationService;
import com.heal.doctor.exception.ConflictException;
import com.heal.doctor.exception.ResourceNotFoundException;
import com.heal.doctor.exception.ValidationException;
import com.heal.doctor.utils.CurrentUserName;
import com.heal.doctor.utils.EmailValidatorUtil;
import com.heal.doctor.services.IEmailService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.Optional;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
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

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final OtpServiceImpl otpService;
    private final INotificationService notificationService;
    private final IDoctorAccountMailService doctorAccountMailService;
    private final IEmailService emailService;
    private final String companyName;
    private final DoctorVerificationRequestRepository doctorVerificationRequestRepository;

    public DoctorServiceImpl(DoctorRepository doctorRepository, UserRepository userRepository,
                            ModelMapper modelMapper,
                            PasswordEncoder passwordEncoder,OtpServiceImpl otpService,
                            INotificationService notificationService,
                            IDoctorAccountMailService doctorAccountMailService,
                            IEmailService emailService,
                            @Value("${company.name}") String companyName,
                            DoctorVerificationRequestRepository doctorVerificationRequestRepository) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.notificationService = notificationService;
        this.doctorAccountMailService = doctorAccountMailService;
        this.emailService = emailService;
        this.companyName = companyName;
        this.doctorVerificationRequestRepository = doctorVerificationRequestRepository;
    }

    @Transactional
    @Override
    public DoctorProfileDTO createDoctor(DoctorRegistrationDTO doctorRegistrationDTO) {
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
                .emailVerified(true)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        UserEntity savedUser = userRepository.save(user);
        logger.info("User created for doctor: userId: {}, email: {}", doctorId, doctorRegistrationDTO.getEmail());

        DoctorEntity doctor = modelMapper.map(doctorRegistrationDTO, DoctorEntity.class);
        doctor.setDoctorId(doctorId); // Link to UserEntity.userId
        doctor.setVerificationStatus(VerificationStatus.PENDING);
        doctor.setCreatedAt(new Date());
        doctor.setUpdatedAt(new Date());
        DoctorEntity savedDoctor = doctorRepository.save(doctor);
        logger.info("Doctor account created successfully: doctorId: {}, email: {}, firstName: {}", 
                savedDoctor.getDoctorId(), savedUser.getEmail(), savedDoctor.getFirstName());
        NotificationEntity notification=NotificationEntity.builder().
                targetId(doctor.getDoctorId()).
                recipientType(NotificationRecipientType.INDIVIDUAL).
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
        
        return modelMapper.map(savedDoctor, DoctorProfileDTO.class);
    }



    @Override
    public DoctorProfileDTO getDoctorById(String doctorId) {
        logger.debug("Fetching doctor by ID: doctorId: {}", doctorId);
        DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));
        UserEntity user = userRepository.findByUserId(doctorId)
                .orElse(null);
        logger.debug("Doctor retrieved: doctorId: {}, email: {}", doctorId, user != null ? user.getEmail() : "N/A");
        DoctorProfileDTO doctorDTO = modelMapper.map(doctor, DoctorProfileDTO.class);
        if (user != null) {
            doctorDTO.setEmail(user.getEmail());
        }
        populatePendingVerificationFields(doctor, doctorDTO);
        return doctorDTO;
    }

    @Override
    public DoctorProfileDTO getDoctorProfile(){
        String username = CurrentUserName.getCurrentUsername();
        String doctorId = CurrentUserName.getCurrentDoctorId();
        logger.debug("Fetching doctor profile: email: {}, doctorId: {}", username, doctorId);
        DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile", doctorId));
        UserEntity user = userRepository.findByEmail(username)
                .orElse(null);
        logger.debug("Doctor profile retrieved: doctorId: {}, email: {}", doctor.getDoctorId(), username);
        DoctorProfileDTO doctorDTO = modelMapper.map(doctor, DoctorProfileDTO.class);
        if (user != null) {
            doctorDTO.setEmail(user.getEmail());
        }
        populatePendingVerificationFields(doctor, doctorDTO);
        return doctorDTO;
    }

    @Override
    public List<DoctorProfileDTO> getAllDoctors(String location, String query) {
        logger.debug("Fetching all doctors with location: {} and query: {}", location, query);
        List<DoctorEntity> doctors;

        if ((location == null || location.isEmpty()) && (query == null || query.isEmpty())) {
            doctors = doctorRepository.findAll();
        } else {
            // Null-safe strings for regex
            String safeLocation = (location != null) ? location : "";
            String safeQuery = (query != null) ? query : "";
            doctors = doctorRepository.findByLocationAndQuery(safeLocation, safeQuery);
        }

        List<DoctorProfileDTO> doctorDTOs = doctors.parallelStream()
                .map(doctor -> modelMapper.map(doctor, DoctorProfileDTO.class))
                .collect(Collectors.toList());
        logger.debug("Retrieved {} doctors", doctorDTOs.size());
        return doctorDTOs;
    }

    @Transactional
    @Override
    public DoctorProfileDTO updateDoctor(UpdateDoctorDetailsDTO updateDoctorDetailsDTO) {
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
        if (updateDoctorDetailsDTO.getAvailability() != null) {
            existingDoctor.setAvailability(updateDoctorDetailsDTO.getAvailability());
        }
        if (updateDoctorDetailsDTO.getClinicAddress() != null && !updateDoctorDetailsDTO.getClinicAddress().isEmpty()) {
            existingDoctor.setClinicAddress(updateDoctorDetailsDTO.getClinicAddress());
        }
        if (updateDoctorDetailsDTO.getClinicName() != null && !updateDoctorDetailsDTO.getClinicName().isEmpty()) {
            existingDoctor.setClinicName(updateDoctorDetailsDTO.getClinicName());
        }
        if (updateDoctorDetailsDTO.getClinicEmail() != null && !updateDoctorDetailsDTO.getClinicEmail().isEmpty()) {
            existingDoctor.setClinicEmail(updateDoctorDetailsDTO.getClinicEmail());
        }
        if (updateDoctorDetailsDTO.getClinicContactNumber() != null && !updateDoctorDetailsDTO.getClinicContactNumber().isEmpty()) {
            existingDoctor.setClinicContactNumber(updateDoctorDetailsDTO.getClinicContactNumber());
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

        boolean isVerificationDetailsChanged = false;
        String licenseNumber = updateDoctorDetailsDTO.getLicenseNumber() != null ? updateDoctorDetailsDTO.getLicenseNumber() : existingDoctor.getLicenseNumber();
        String licensingAuthority = updateDoctorDetailsDTO.getLicensingAuthority() != null ? updateDoctorDetailsDTO.getLicensingAuthority() : existingDoctor.getLicensingAuthority();
        Date licenseExpiryDate = updateDoctorDetailsDTO.getLicenseExpiryDate() != null ? updateDoctorDetailsDTO.getLicenseExpiryDate() : existingDoctor.getLicenseExpiryDate();

        if (updateDoctorDetailsDTO.getLicenseNumber() != null || updateDoctorDetailsDTO.getLicensingAuthority() != null || updateDoctorDetailsDTO.getLicenseExpiryDate() != null) {
            isVerificationDetailsChanged = true;
        }

        if (isVerificationDetailsChanged) {
            DoctorVerificationRequestEntity request = doctorVerificationRequestRepository
                    .findFirstByDoctorIdAndStatusOrderBySubmittedAtDesc(doctorId, RequestStatus.PENDING)
                    .orElse(null);

            if (request == null) {
                request = DoctorVerificationRequestEntity.builder()
                        .doctorId(doctorId)
                        .status(RequestStatus.PENDING)
                        .submittedAt(new Date())
                        .build();
            }

            request.setLicenseNumber(licenseNumber);
            request.setLicensingAuthority(licensingAuthority);
            request.setLicenseExpiryDate(licenseExpiryDate);
            request.setSubmittedAt(new Date());
            doctorVerificationRequestRepository.save(request);

            // Send in-app notification
            String title = "Verification Request Under Review";
            String message = "Your updated medical license details (License No: " + licenseNumber + ") have been submitted and are currently pending administrative review.";
            
            NotificationEntity notification = NotificationEntity.builder()
                    .targetId(doctorId)
                    .recipientType(NotificationRecipientType.INDIVIDUAL)
                    .type(NotificationType.SYSTEM)
                    .title(title)
                    .message(message)
                    .createdAt(java.time.Instant.now())
                    .build();

            notificationService.createNotificationAsync(notification).exceptionally(ex -> {
                logger.error("Failed to create verification submission notification asynchronously for doctorId: {}, error: {}", 
                        doctorId, ex.getMessage(), ex);
                return null;
            });

            // Send HTML email
            try {
                String doctorEmail = existingDoctor.getClinicEmail();
                Optional<UserEntity> userOpt = userRepository.findByUserId(doctorId);
                if (userOpt.isPresent()) {
                    doctorEmail = userOpt.get().getEmail();
                }

                if (doctorEmail != null && !doctorEmail.isEmpty()) {
                    final String toEmail = doctorEmail;
                    final String doctorName = existingDoctor.getFirstName() + " " + existingDoctor.getLastName();
                    final String finalLicenseNumber = licenseNumber;
                    final String finalLicensingAuthority = licensingAuthority;
                    final String expiryDateStr = licenseExpiryDate != null ? licenseExpiryDate.toString() : "N/A";

                    emailService.sendHtmlEmail(
                            toEmail,
                            "Verification Request Received - " + companyName,
                            "license-submitted.template.html",
                            Map.of(
                                    "companyName", companyName,
                                    "doctorName", doctorName,
                                    "licenseNumber", finalLicenseNumber,
                                    "licensingAuthority", finalLicensingAuthority,
                                    "expiryDate", expiryDateStr
                            )
                    ).exceptionally(ex -> {
                        logger.error("Failed to send verification submission email to doctorId: {}, email: {}, error: {}", 
                                doctorId, toEmail, ex.getMessage());
                        return null;
                    });
                }
            } catch (Exception ex) {
                logger.error("Error setting up verification submission email for doctorId: {}", doctorId, ex);
            }
        }

        existingDoctor.setUpdatedAt(new Date());

        DoctorEntity updatedDoctor = doctorRepository.save(existingDoctor);

        DoctorProfileDTO doctorDTO = new DoctorProfileDTO();
        modelMapper.map(updatedDoctor, doctorDTO);
        populatePendingVerificationFields(updatedDoctor, doctorDTO);

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

    private String generateDoctorId() {
        String timestamp = new SimpleDateFormat(DOCTOR_ID_DATE_FORMAT).format(new Date());
        String randomNumber = String.format(DOCTOR_ID_RANDOM_FORMAT, new Random().nextInt(DOCTOR_ID_RANDOM_RANGE));
        return String.format("%s-%s-%s", DOCTOR_ID_PREFIX, timestamp, randomNumber);
    }



    @Transactional
    @Override
    public DoctorProfileDTO updateVerificationStatus(String doctorId, VerificationStatus status) {
        logger.info("Updating doctor verification status: doctorId: {}, status: {}", doctorId, status);
        DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));

        VerificationStatus oldStatus = doctor.getVerificationStatus();
        
        String licenseNumber = doctor.getLicenseNumber() != null ? doctor.getLicenseNumber() : "N/A";
        String licensingAuthority = doctor.getLicensingAuthority() != null ? doctor.getLicensingAuthority() : "N/A";
        String expiryDateStr = doctor.getLicenseExpiryDate() != null ? doctor.getLicenseExpiryDate().toString() : "N/A";

        if (status == VerificationStatus.VERIFIED) {
            Optional<DoctorVerificationRequestEntity> pendingOpt = doctorVerificationRequestRepository
                    .findFirstByDoctorIdAndStatusOrderBySubmittedAtDesc(doctorId, RequestStatus.PENDING);
            if (pendingOpt.isPresent()) {
                DoctorVerificationRequestEntity pending = pendingOpt.get();
                pending.setStatus(RequestStatus.APPROVED);
                pending.setReviewedAt(new Date());
                doctorVerificationRequestRepository.save(pending);

                doctor.setLicenseNumber(pending.getLicenseNumber());
                doctor.setLicensingAuthority(pending.getLicensingAuthority());
                doctor.setLicenseExpiryDate(pending.getLicenseExpiryDate());

                licenseNumber = pending.getLicenseNumber();
                licensingAuthority = pending.getLicensingAuthority();
                expiryDateStr = pending.getLicenseExpiryDate() != null ? pending.getLicenseExpiryDate().toString() : "N/A";
            }
            doctor.setVerificationStatus(VerificationStatus.VERIFIED);
        } else if (status == VerificationStatus.REJECTED || status == VerificationStatus.DENIED) {
            Optional<DoctorVerificationRequestEntity> pendingOpt = doctorVerificationRequestRepository
                    .findFirstByDoctorIdAndStatusOrderBySubmittedAtDesc(doctorId, RequestStatus.PENDING);
            if (pendingOpt.isPresent()) {
                DoctorVerificationRequestEntity pending = pendingOpt.get();
                pending.setStatus(RequestStatus.REJECTED);
                pending.setReviewedAt(new Date());
                doctorVerificationRequestRepository.save(pending);

                licenseNumber = pending.getLicenseNumber();
                licensingAuthority = pending.getLicensingAuthority();
                expiryDateStr = pending.getLicenseExpiryDate() != null ? pending.getLicenseExpiryDate().toString() : "N/A";
            }
            if (doctor.getVerificationStatus() != VerificationStatus.VERIFIED) {
                doctor.setVerificationStatus(status);
            }
        } else {
            doctor.setVerificationStatus(status);
        }

        doctor.setUpdatedAt(new Date());
        DoctorEntity savedDoctor = doctorRepository.save(doctor);

        if (oldStatus != doctor.getVerificationStatus()) {
            String title;
            String message;
            String templateName;
            String subject;

            if (doctor.getVerificationStatus() == VerificationStatus.VERIFIED) {
                title = "Practice Account Verified";
                message = "Congratulations! Your practice account has been verified successfully. You can now configure availability and accept appointments.";
                templateName = "license-verified.template.html";
                subject = "Practice Account Verified - " + companyName;
            } else if (doctor.getVerificationStatus() == VerificationStatus.REJECTED || doctor.getVerificationStatus() == VerificationStatus.DENIED) {
                title = "Verification Rejected";
                message = "Your verification request has been rejected or denied. Please review your credentials and re-submit.";
                templateName = "license-rejected.template.html";
                subject = "Practice Verification Rejected - " + companyName;
            } else if (doctor.getVerificationStatus() == VerificationStatus.SUSPENDED) {
                title = "Practice Account Suspended";
                message = "Your practice account verification has been suspended. Please check your license status or contact support.";
                templateName = "license-suspended.template.html";
                subject = "Practice Account Suspended - " + companyName;
            } else if (doctor.getVerificationStatus() == VerificationStatus.TERMINATED) {
                title = "Practice Account Terminated";
                message = "Your practice account verification has been terminated. Please contact support if you believe this is an error.";
                templateName = "license-terminated.template.html";
                subject = "Practice Account Terminated - " + companyName;
            } else {
                title = "Verification Status Update";
                message = "Your verification status has been updated to " + doctor.getVerificationStatus() + ".";
                templateName = null;
                subject = null;
            }

            NotificationEntity notification = NotificationEntity.builder()
                    .targetId(doctorId)
                    .recipientType(NotificationRecipientType.INDIVIDUAL)
                    .type(NotificationType.SYSTEM)
                    .title(title)
                    .message(message)
                    .createdAt(java.time.Instant.now())
                    .build();

            notificationService.createNotificationAsync(notification).exceptionally(ex -> {
                logger.error("Failed to create verification status notification asynchronously for doctorId: {}, error: {}", 
                        doctorId, ex.getMessage(), ex);
                return null;
            });

            if (templateName != null) {
                try {
                    String doctorEmail = doctor.getClinicEmail();
                    Optional<UserEntity> userOpt = userRepository.findByUserId(doctorId);
                    if (userOpt.isPresent()) {
                        doctorEmail = userOpt.get().getEmail();
                    }

                    if (doctorEmail != null && !doctorEmail.isEmpty()) {
                        final String toEmail = doctorEmail;
                        final String doctorName = doctor.getFirstName() + " " + doctor.getLastName();
                        final String finalLicenseNumber = licenseNumber;
                        final String finalLicensingAuthority = licensingAuthority;
                        final String finalExpiryDateStr = expiryDateStr;

                        emailService.sendHtmlEmail(
                                toEmail,
                                subject,
                                templateName,
                                Map.of(
                                        "companyName", companyName,
                                        "doctorName", doctorName,
                                        "licenseNumber", finalLicenseNumber,
                                        "licensingAuthority", finalLicensingAuthority,
                                        "expiryDate", finalExpiryDateStr
                                )
                        ).exceptionally(ex -> {
                            logger.error("Failed to send verification status email to doctorId: {}, email: {}, error: {}", 
                                    doctorId, toEmail, ex.getMessage());
                            return null;
                        });
                    }
                } catch (Exception ex) {
                    logger.error("Error setting up verification status email for doctorId: {}", doctorId, ex);
                }
            }
        }

        DoctorProfileDTO doctorDTO = new DoctorProfileDTO();
        modelMapper.map(savedDoctor, doctorDTO);
        populatePendingVerificationFields(savedDoctor, doctorDTO);
        return doctorDTO;
    }

    private void populatePendingVerificationFields(DoctorEntity doctor, DoctorProfileDTO dto) {
        doctorVerificationRequestRepository.findFirstByDoctorIdAndStatusOrderBySubmittedAtDesc(doctor.getDoctorId(), RequestStatus.PENDING)
                .ifPresent(req -> {
                    dto.setHasPendingVerification(true);
                    dto.setPendingLicenseNumber(req.getLicenseNumber());
                    dto.setPendingLicensingAuthority(req.getLicensingAuthority());
                    dto.setPendingLicenseExpiryDate(req.getLicenseExpiryDate());
                });
    }

}

