package com.heal.doctor.services.impl;

import com.heal.doctor.Mail.IDoctorAccountMailService;
import com.heal.doctor.Mail.impl.OtpServiceImpl;
import com.heal.doctor.dto.*;
import com.heal.doctor.models.DoctorEntity;
import com.heal.doctor.models.NotificationEntity;
import com.heal.doctor.models.enums.AvailableDayEnum;
import com.heal.doctor.models.enums.NotificationRecipientType;
import com.heal.doctor.models.enums.NotificationType;
import com.heal.doctor.models.enums.RolesEnum;
import com.heal.doctor.models.UserEntity;
import com.heal.doctor.repositories.DoctorRepository;
import com.heal.doctor.repositories.UserRepository;
import com.heal.doctor.services.IDoctorService;
import com.heal.doctor.services.INotificationService;
import com.heal.doctor.exception.ConflictException;
import com.heal.doctor.exception.ResourceNotFoundException;
import com.heal.doctor.exception.ValidationException;
import com.heal.doctor.utils.CurrentUserName;
import com.heal.doctor.utils.EmailValidatorUtil;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public DoctorServiceImpl(DoctorRepository doctorRepository, UserRepository userRepository,
                            ModelMapper modelMapper,
                            PasswordEncoder passwordEncoder,OtpServiceImpl otpService,
                            INotificationService notificationService,
                            IDoctorAccountMailService doctorAccountMailService) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.notificationService = notificationService;
        this.doctorAccountMailService = doctorAccountMailService;
    }

    @Transactional
    @Override
    public UserDTO createDoctor(DoctorRegistrationDTO doctorRegistrationDTO) {
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
        
        return modelMapper.map(savedDoctor, UserDTO.class);
    }



    @Override
    public UserDTO getDoctorById(String doctorId) {
        logger.debug("Fetching doctor by ID: doctorId: {}", doctorId);
        DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));
        UserEntity user = userRepository.findByUserId(doctorId)
                .orElse(null);
        logger.debug("Doctor retrieved: doctorId: {}, email: {}", doctorId, user != null ? user.getEmail() : "N/A");
        UserDTO doctorDTO = modelMapper.map(doctor, UserDTO.class);
        if (user != null) {
            doctorDTO.setEmail(user.getEmail());
        }
        return doctorDTO;
    }

    @Override
    public UserDTO getDoctorProfile(){
        String username = CurrentUserName.getCurrentUsername();
        String doctorId = CurrentUserName.getCurrentDoctorId();
        logger.debug("Fetching doctor profile: email: {}, doctorId: {}", username, doctorId);
        DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile", doctorId));
        UserEntity user = userRepository.findByEmail(username)
                .orElse(null);
        logger.debug("Doctor profile retrieved: doctorId: {}, email: {}", doctor.getDoctorId(), username);
        UserDTO doctorDTO = modelMapper.map(doctor, UserDTO.class);
        if (user != null) {
            doctorDTO.setEmail(user.getEmail());
        }
        return doctorDTO;
    }

    @Override
    public List<UserDTO> getAllDoctors() {
        logger.debug("Fetching all doctors");
        List<UserDTO> doctors = doctorRepository.findAll().parallelStream()
                .map(doctor -> modelMapper.map(doctor, UserDTO.class))
                .collect(Collectors.toList());
        logger.debug("Retrieved {} doctors", doctors.size());
        return doctors;
    }

    @Transactional
    @Override
    public UserDTO updateDoctor(UpdateDoctorDetailsDTO updateDoctorDetailsDTO) {
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

        existingDoctor.setUpdatedAt(new Date());

        DoctorEntity updatedDoctor = doctorRepository.save(existingDoctor);

        UserDTO doctorDTO = new UserDTO();
        modelMapper.map(updatedDoctor, doctorDTO);

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

    private void validateAvailableDays(List<AvailableDayEnum> availableDays) {
        for (AvailableDayEnum day : availableDays) {
            if (day == null) {
                throw new ValidationException("Invalid day in available days");
            }
        }
    }

}

