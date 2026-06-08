package com.heal.doctor.services;

import com.heal.doctor.dto.*;
import com.heal.doctor.models.enums.VerificationStatus;

import java.util.List;

public interface IDoctorService {
    DoctorProfileDTO createDoctor(DoctorRegistrationDTO doctorRegistrationDTO);
    DoctorProfileDTO getDoctorById(String doctorId);
    DoctorProfileDTO getDoctorProfile();
    List<DoctorProfileDTO> getAllDoctors(String location, String query);

    DoctorProfileDTO updateDoctor(UpdateDoctorDetailsDTO updateDoctorDetailsDTO);
    DoctorProfileDTO updateVerificationStatus(String doctorId, VerificationStatus status);

    void deleteDoctor(String doctorId);
}
