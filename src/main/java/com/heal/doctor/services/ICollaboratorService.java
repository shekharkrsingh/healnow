package com.heal.doctor.services;

import com.heal.doctor.dto.CollaboratorDTO;
import com.heal.doctor.dto.CollaboratorProfileDTO;
import com.heal.doctor.dto.UpdateCollaboratorProfileDTO;

import java.util.List;

public interface ICollaboratorService {
    List<CollaboratorDTO> getCollaboratorsByDoctor(String doctorId);
    void deactivateCollaborator(String collaboratorId, String doctorId);
    void activateCollaborator(String collaboratorId, String doctorId);
    void removeCollaborator(String collaboratorId, String doctorId);
    CollaboratorProfileDTO getCollaboratorProfile();
    CollaboratorProfileDTO updateCollaboratorProfile(UpdateCollaboratorProfileDTO updateDTO);
}
