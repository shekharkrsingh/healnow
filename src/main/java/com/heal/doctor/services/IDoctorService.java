package com.heal.doctor.services;

import com.heal.doctor.dto.*;

import java.util.List;

public interface IDoctorService {
    DoctorProfileDTO createDoctor(DoctorRegistrationDTO doctorRegistrationDTO);
    DoctorProfileDTO getDoctorById(String doctorId);
    DoctorProfileDTO getDoctorProfile();
    List<DoctorProfileDTO> getAllDoctors();

    DoctorProfileDTO updateDoctor(UpdateDoctorDetailsDTO updateDoctorDetailsDTO);

    void deleteDoctor(String doctorId);
}
