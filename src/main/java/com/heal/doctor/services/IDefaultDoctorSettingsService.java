package com.heal.doctor.services;

import com.heal.doctor.dto.DefaultDoctorSettingsDTO;

public interface IDefaultDoctorSettingsService {
    DefaultDoctorSettingsDTO getSettings();
    DefaultDoctorSettingsDTO updateSettings(DefaultDoctorSettingsDTO settingsDTO);
}
