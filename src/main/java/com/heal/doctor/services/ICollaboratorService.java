package com.heal.doctor.services;

import com.heal.doctor.dto.CollaboratorDTO;
import com.heal.doctor.dto.DoctorProfileDTO;
import com.heal.doctor.dto.UpdateCollaboratorProfileDTO;

import java.util.List;

public interface ICollaboratorService {
    List<CollaboratorDTO> getCollaboratorsByDoctor(String doctorId);
    void deactivateCollaborator(String collaboratorId);
    void activateCollaborator(String collaboratorId);
    void removeCollaborator(String collaboratorId);
    DoctorProfileDTO getCollaboratorProfile();
    DoctorProfileDTO updateCollaboratorProfile(UpdateCollaboratorProfileDTO updateDTO);
}
