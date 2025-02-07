package com.heal.doctor.services.impl;

import com.heal.doctor.dto.*;
import com.heal.doctor.models.DoctorEntity;
import com.heal.doctor.repositories.DoctorRepository;
import com.heal.doctor.security.JwtUtil;
import com.heal.doctor.services.IDoctorService;
import com.heal.doctor.utils.EmailValidatorUtil;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

        DoctorEntity doctor = modelMapper.map(doctorRegistrationDTO, DoctorEntity.class);
        doctor.setPassword(passwordEncoder.encode(doctorRegistrationDTO.getPassword()));
        doctor.setDoctorId(generateDoctorId());
        DoctorEntity savedDoctor = doctorRepository.save(doctor);
        return modelMapper.map(savedDoctor, DoctorDTO.class);
    }

    public String loginDoctor(String username, String password) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username, password);

        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        return jwtUtil.generateToken(userDetails.getUsername());
    }

    @Override
    public DoctorDTO getDoctorById(String doctorId) {
        DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor profile not found"));
        return modelMapper.map(doctor, DoctorDTO.class);
    }

    @Override
    public DoctorDTO getDoctorProfile(){
        DoctorEntity doctor = doctorRepository.findByEmail(getCurrentUsername())
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
    public DoctorDTO updateDoctor( DoctorDTO doctorDTO) {
        String username=getCurrentUsername();
        DoctorEntity existingDoctor = doctorRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        modelMapper.map(doctorDTO, existingDoctor);

        DoctorEntity updatedDoctor = doctorRepository.save(existingDoctor);
        return modelMapper.map(updatedDoctor, DoctorDTO.class);
    }



    @Override
    public void deleteDoctor(String doctorId) {
        DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        doctorRepository.delete(doctor);
    }


    @Override
    public void changePassword(ChangePasswordDTO changePasswordDTO) {
        DoctorEntity doctor = doctorRepository.findByEmail(getCurrentUsername())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        if (!passwordEncoder.matches(changePasswordDTO.getOldPassword(), doctor.getPassword())) {
            throw new IllegalArgumentException("Invalid old password");
        }

        doctor.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
        doctorRepository.save(doctor);
    }

    @Override
    public void updateEmail(UpdateEmailDTO updateEmailDTO) {
        DoctorEntity doctor = doctorRepository.findByEmail(getCurrentUsername())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        if (!passwordEncoder.matches(updateEmailDTO.getPassword(), doctor.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }

//        if (!otpService.verifyOTP(updateEmailDTO.getNewEmail(), updateEmailDTO.getOtp())) {
//            throw new IllegalArgumentException("Invalid OTP");
//        }

        doctor.setEmail(updateEmailDTO.getNewEmail());
        doctorRepository.save(doctor);
    }

    @Override
    public void forgotPassword(ForgotPasswordDTO forgotPasswordDTO) {
        DoctorEntity doctor = doctorRepository.findByEmail(forgotPasswordDTO.getEmail())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

//        if (!otpService.verifyOTP(forgotPasswordDTO.getEmail(), forgotPasswordDTO.getOtp())) {
//            throw new IllegalArgumentException("Invalid OTP");
//        }

        doctor.setPassword(passwordEncoder.encode(forgotPasswordDTO.getNewPassword()));
        doctorRepository.save(doctor);
    }


    private String generateDoctorId() {
        String prefix = "DOC";
        String timestamp = new SimpleDateFormat("yyMMdd-HHmm").format(new Date());
        String randomNumber = String.format("%03d", new Random().nextInt(1000));
        return String.format("%s-%s-%s", prefix, timestamp, randomNumber);
    }

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() instanceof String username) {
            return username;
        }
        return null;
    }

}

