package com.heal.doctor.services;

import com.heal.doctor.dto.AssociatedDoctorDTO;
import com.heal.doctor.dto.DoctorProfileDTO;

import java.util.List;

public interface ICollaboratorDoctorService {
    List<AssociatedDoctorDTO> getAssociatedDoctors();
    AssociatedDoctorDTO switchActiveDoctor(String doctorId);
    DoctorProfileDTO getActiveDoctorProfile();
}
