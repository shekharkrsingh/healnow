package com.heal.doctor.services;

import com.heal.doctor.dto.*;

import java.util.List;

public interface IDoctorService {
    UserDTO createDoctor(DoctorRegistrationDTO doctorRegistrationDTO);
    UserDTO getDoctorById(String doctorId);
    UserDTO getDoctorProfile();
    List<UserDTO> getAllDoctors();

    UserDTO updateDoctor(UpdateDoctorDetailsDTO updateDoctorDetailsDTO);

    void deleteDoctor(String doctorId);
}
