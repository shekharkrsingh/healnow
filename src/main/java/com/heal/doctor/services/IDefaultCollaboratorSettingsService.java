package com.heal.doctor.services;

import com.heal.doctor.dto.DefaultCollaboratorSettingsDTO;

public interface IDefaultCollaboratorSettingsService {
    DefaultCollaboratorSettingsDTO getSettings();
    DefaultCollaboratorSettingsDTO updateSettings(DefaultCollaboratorSettingsDTO settingsDTO);
}
