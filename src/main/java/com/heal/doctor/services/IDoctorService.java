package com.heal.doctor.services;

import com.heal.doctor.dto.*;

import java.util.List;

public interface IDoctorService {
    DoctorDTO createDoctor(DoctorRegistrationDTO doctorRegistrationDTO);
    DoctorDTO getDoctorById(String doctorId);
    DoctorDTO getDoctorProfile();
    List<DoctorDTO> getAllDoctors();

    DoctorDTO updateDoctor(UpdateDoctorDetailsDTO updateDoctorDetailsDTO);

    void deleteDoctor(String doctorId);
    void forgotPassword(ForgotPasswordDTO forgotPasswordDTO);
    String updateEmail( UpdateEmailDTO updateEmailDTO);
    void changePassword(ChangePasswordDTO changePasswordDTO);
    String loginDoctor(String username, String password);
}
