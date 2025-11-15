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
        if (!EmailValidatorUtil.isValidEmail(doctorRegistrationDTO.getEmail())) {
            throw new ValidationException("Invalid email format");
        }
        if (doctorRepository.findByEmail(doctorRegistrationDTO.getEmail()).isPresent()) {
            throw new ConflictException("Doctor", "A doctor with this email already exists");
        }
        if(doctorRegistrationDTO.getFirstName()==null || doctorRegistrationDTO.getPassword()==null){
            throw new ValidationException("First name and password are required");
        }
        if(doctorRegistrationDTO.getOtp()==null){
            throw new ValidationException("OTP is required");
        }

        otpService.validateOtp(doctorRegistrationDTO.getEmail(), doctorRegistrationDTO.getOtp());

        DoctorEntity doctor = modelMapper.map(doctorRegistrationDTO, DoctorEntity.class);
        doctor.setPassword(passwordEncoder.encode(doctorRegistrationDTO.getPassword()));
        doctor.setCreatedAt(new Date());
        doctor.setUpdatedAt(new Date());
        doctor.setDoctorId(generateDoctorId());
        DoctorEntity savedDoctor = doctorRepository.save(doctor);
        NotificationEntity notification=NotificationEntity.builder().
                doctorId(savedDoctor.getDoctorId()).
                type(NotificationType.SYSTEM).
                title("Welcome "+ savedDoctor.getFirstName()).
                message("Your account has been successfully created. Complete your profile to start managing appointments and providing care.").
                build();
        notificationService.createNotification(notification);
        doctorAccountMailService.doctorWelcomeMail(savedDoctor.getFirstName(), savedDoctor.getEmail());
        
        return modelMapper.map(savedDoctor, DoctorDTO.class);
    }


    public String loginDoctor(String username, String password) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username, password);

        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        DoctorUserDetails userDetails = (DoctorUserDetails) userDetailsService.loadUserByUsername(username);

        return jwtUtil.generateToken(userDetails.getUsername(), userDetails.getDoctorId());
    }


    @Override
    public DoctorDTO getDoctorById(String doctorId) {
        DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));
        return modelMapper.map(doctor, DoctorDTO.class);
    }

    @Override
    public DoctorDTO getDoctorProfile(){
        DoctorEntity doctor = doctorRepository.findByEmail(CurrentUserName.getCurrentUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile", CurrentUserName.getCurrentUsername()));
        return modelMapper.map(doctor, DoctorDTO.class);
    }

    @Override
    public List<DoctorDTO> getAllDoctors() {
        return doctorRepository.findAll().stream()
                .map(doctor -> modelMapper.map(doctor, DoctorDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public DoctorDTO updateDoctor(UpdateDoctorDetailsDTO updateDoctorDetailsDTO) {
        String username = CurrentUserName.getCurrentUsername();
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
            existingDoctor.setPhoneNumber(updateDoctorDetailsDTO.getPhoneNumber());
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
        DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));
        doctorRepository.delete(doctor);
    }


    @Override
    public void changePassword(ChangePasswordDTO changePasswordDTO) {
        DoctorEntity doctor = doctorRepository.findByEmail(CurrentUserName.getCurrentUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", CurrentUserName.getCurrentUsername()));

        if (!passwordEncoder.matches(changePasswordDTO.getOldPassword(), doctor.getPassword())) {
            throw new UnauthorizedException("Invalid old password");
        }
        doctor.setUpdatedAt(new Date());
        doctor.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
        DoctorEntity savedDoctor= doctorRepository.save(doctor);
        NotificationEntity notification=NotificationEntity.builder()
                .doctorId(savedDoctor.getDoctorId())
                .type(NotificationType.INFO)
                .title("Password Updated.")
                .message("Your login credentials have been updated.")
                .build();
        notificationService.createNotification(notification);
        doctorAccountMailService.doctorPasswordChangeMail(savedDoctor.getFirstName(), savedDoctor.getEmail());
    }

    @Override
    public String updateEmail(UpdateEmailDTO updateEmailDTO) {
        DoctorEntity doctor = doctorRepository.findByEmail(CurrentUserName.getCurrentUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", CurrentUserName.getCurrentUsername()));
        Optional<DoctorEntity> doctorIsAlreadyAvailable = doctorRepository.findByEmail(updateEmailDTO.getNewEmail());

        if (doctorIsAlreadyAvailable.isPresent()) {
            throw new ConflictException("Email", "A doctor with email '" + updateEmailDTO.getNewEmail() + "' already exists");
        }

        if (!passwordEncoder.matches(updateEmailDTO.getPassword(), doctor.getPassword())) {
            throw new UnauthorizedException("Invalid password");
        }

        if (!otpService.validateOtp(updateEmailDTO.getNewEmail(), updateEmailDTO.getOtp())) {
            throw new BadRequestException("Invalid OTP");
        }
        String oldMail=doctor.getEmail();
        doctor.setEmail(updateEmailDTO.getNewEmail());
        doctor.setUpdatedAt(new Date());
        DoctorEntity savedDoctor =doctorRepository.save(doctor);
        NotificationEntity notification=NotificationEntity.builder()
                .doctorId(savedDoctor.getDoctorId())
                .type(NotificationType.INFO)
                .title("Security Update")
                .message("Your login email has been changed. If this wasn’t you, please review your security settings.")
                .build();
        notificationService.createNotification(notification);
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
        DoctorEntity doctor = doctorRepository.findByEmail(forgotPasswordDTO.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", forgotPasswordDTO.getEmail()));

        if (!otpService.validateOtp(forgotPasswordDTO.getEmail(), forgotPasswordDTO.getOtp())) {
            throw new BadRequestException("Invalid OTP");
        }

        doctor.setPassword(passwordEncoder.encode(forgotPasswordDTO.getNewPassword()));
        doctor.setUpdatedAt(new Date());
        DoctorEntity savedDoctor= doctorRepository.save(doctor);
        NotificationEntity notification=NotificationEntity.builder()
                .doctorId(savedDoctor.getDoctorId())
                .type(NotificationType.INFO)
                .title("Password Updated.")
                .message("Your login credentials have been updated.")
                .build();
        notificationService.createNotification(notification);
        doctorAccountMailService.doctorPasswordChangeMail(
                savedDoctor.getFirstName(),
                savedDoctor.getEmail()
        );
    }


    private String generateDoctorId() {
        String prefix = "DOC";
        String timestamp = new SimpleDateFormat("yyMMdd-HHmm").format(new Date());
        String randomNumber = String.format("%03d", new Random().nextInt(1000));
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

