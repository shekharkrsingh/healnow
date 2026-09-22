package com.heal.doctor.services;

import com.heal.doctor.dto.CollaboratorSettingsDTO;

public interface ICollaboratorSettingsService {
    CollaboratorSettingsDTO getSettingsByCollaboratorId(String collaboratorId);
    CollaboratorSettingsDTO updateSettings(String collaboratorId, CollaboratorSettingsDTO settingsDTO);
}
