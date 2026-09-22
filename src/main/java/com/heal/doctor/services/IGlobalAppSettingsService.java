package com.heal.doctor.services;

import com.heal.doctor.dto.GlobalAppSettingsDTO;

public interface IGlobalAppSettingsService {
    GlobalAppSettingsDTO getSettings();
    GlobalAppSettingsDTO updateSettings(GlobalAppSettingsDTO settingsDTO);
}
