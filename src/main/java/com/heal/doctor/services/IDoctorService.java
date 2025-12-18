package com.heal.doctor.services;

import com.heal.doctor.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IDoctorService {
    UserDTO createDoctor(DoctorRegistrationDTO doctorRegistrationDTO);
    UserDTO getDoctorById(String doctorId);
    UserDTO getDoctorProfile();
    List<UserDTO> getAllDoctors();

    UserDTO updateDoctor(UpdateDoctorDetailsDTO updateDoctorDetailsDTO);

    void deleteDoctor(String doctorId);
    void forgotPassword(ForgotPasswordDTO forgotPasswordDTO);
    String updateEmail( UpdateEmailDTO updateEmailDTO);
    void changePassword(ChangePasswordDTO changePasswordDTO);
    String loginDoctor(String username, String password);
    String changeProfilePicture(MultipartFile file);
    String changeCoverPicture(MultipartFile file);
}
