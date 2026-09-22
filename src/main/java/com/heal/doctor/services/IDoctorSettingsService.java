package com.heal.doctor.services;

import com.heal.doctor.dto.DoctorSettingsDTO;

public interface IDoctorSettingsService {
    DoctorSettingsDTO getSettingsByDoctorId(String doctorId);
    DoctorSettingsDTO updateSettings(String doctorId, DoctorSettingsDTO settingsDTO);
}
