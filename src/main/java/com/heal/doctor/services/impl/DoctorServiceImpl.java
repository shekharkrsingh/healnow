package com.heal.doctor.services.impl;

import com.heal.doctor.dto.*;
import com.heal.doctor.models.DoctorEntity;
import com.heal.doctor.models.enums.AvailableDayEnum;
import com.heal.doctor.repositories.DoctorRepository;
import com.heal.doctor.security.DoctorUserDetails;
import com.heal.doctor.security.JwtUtil;
import com.heal.doctor.services.IDoctorService;
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

    @Transactional
    @Override
    public DoctorDTO createDoctor(DoctorRegistrationDTO doctorRegistrationDTO) {
        if (!EmailValidatorUtil.isValidEmail(doctorRegistrationDTO.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (doctorRepository.findByEmail(doctorRegistrationDTO.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Doctor with this email already exists");
        }
        if(doctorRegistrationDTO.getFirstName()==null || doctorRegistrationDTO.getPassword()==null){
            throw new IllegalArgumentException("First Name and Password are required");
        }
        if(doctorRegistrationDTO.getOtp()==null){
            throw new IllegalArgumentException("OTP is required");
        }

        otpService.validateOtp(doctorRegistrationDTO.getEmail(), doctorRegistrationDTO.getOtp());

        DoctorEntity doctor = modelMapper.map(doctorRegistrationDTO, DoctorEntity.class);
        doctor.setPassword(passwordEncoder.encode(doctorRegistrationDTO.getPassword()));
        doctor.setCreatedAt(new Date());
        doctor.setUpdatedAt(new Date());
        doctor.setDoctorId(generateDoctorId());
        DoctorEntity savedDoctor = doctorRepository.save(doctor);
        
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
                .orElseThrow(() -> new RuntimeException("Doctor profile not found"));
        return modelMapper.map(doctor, DoctorDTO.class);
    }

    @Override
    public DoctorDTO getDoctorProfile(){
        DoctorEntity doctor = doctorRepository.findByEmail(CurrentUserName.getCurrentUsername())
                .orElseThrow(() -> new RuntimeException("Your profile not found"));
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
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

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
        modelMapper.map(existingDoctor, doctorDTO);

        return doctorDTO;
    }





    @Override
    public void deleteDoctor(String doctorId) {
        DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        doctorRepository.delete(doctor);
    }


    @Override
    public void changePassword(ChangePasswordDTO changePasswordDTO) {
        DoctorEntity doctor = doctorRepository.findByEmail(CurrentUserName.getCurrentUsername())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        if (!passwordEncoder.matches(changePasswordDTO.getOldPassword(), doctor.getPassword())) {
            throw new IllegalArgumentException("Invalid old password");
        }
        doctor.setUpdatedAt(new Date());
        doctor.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
        doctorRepository.save(doctor);
    }

    @Override
    public void updateEmail(UpdateEmailDTO updateEmailDTO) {
        DoctorEntity doctor = doctorRepository.findByEmail(CurrentUserName.getCurrentUsername())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        if (!passwordEncoder.matches(updateEmailDTO.getPassword(), doctor.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }

        if (!otpService.validateOtp(updateEmailDTO.getNewEmail(), updateEmailDTO.getOtp())) {
            throw new IllegalArgumentException("Invalid OTP");
        }
        doctor.setEmail(updateEmailDTO.getNewEmail());
        doctor.setUpdatedAt(new Date());
        doctorRepository.save(doctor);
    }

    @Override
    public void forgotPassword(ForgotPasswordDTO forgotPasswordDTO) {
        DoctorEntity doctor = doctorRepository.findByEmail(forgotPasswordDTO.getEmail())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        if (!otpService.validateOtp(forgotPasswordDTO.getEmail(), forgotPasswordDTO.getOtp())) {
            throw new IllegalArgumentException("Invalid OTP");
        }

        doctor.setPassword(passwordEncoder.encode(forgotPasswordDTO.getNewPassword()));
        doctor.setUpdatedAt(new Date());
        doctorRepository.save(doctor);
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
                throw new IllegalArgumentException("Invalid day in available days.");
            }
        }
    }

}

