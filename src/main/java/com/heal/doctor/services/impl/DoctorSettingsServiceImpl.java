package com.heal.doctor.services.impl;

import com.heal.doctor.dto.DoctorSettingsDTO;
import com.heal.doctor.models.DoctorSettingsEntity;
import com.heal.doctor.repositories.DoctorSettingsRepository;
import com.heal.doctor.services.IDoctorSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DoctorSettingsServiceImpl implements IDoctorSettingsService {

    private final DoctorSettingsRepository repository;

    @Override
    public DoctorSettingsDTO getSettingsByDoctorId(String doctorId) {
        Optional<DoctorSettingsEntity> optionalSettings = repository.findByDoctorId(doctorId);
        
        if (optionalSettings.isEmpty()) {
            return DoctorSettingsDTO.builder()
                    .doctorId(doctorId)
                    .publicBookingAllowed(true) // default values
                    .enableEmergencyFeature(false) // default values
                    .build(); 
        }
        
        DoctorSettingsEntity entity = optionalSettings.get();
        return DoctorSettingsDTO.builder()
                .doctorId(entity.getDoctorId())
                .publicBookingAllowed(entity.isPublicBookingAllowed())
                .enableEmergencyFeature(entity.isEnableEmergencyFeature())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public DoctorSettingsDTO updateSettings(String doctorId, DoctorSettingsDTO settingsDTO) {
        DoctorSettingsEntity entity = repository.findByDoctorId(doctorId)
                .orElse(DoctorSettingsEntity.builder().doctorId(doctorId).build());
        
        entity.setPublicBookingAllowed(settingsDTO.isPublicBookingAllowed());
        entity.setEnableEmergencyFeature(settingsDTO.isEnableEmergencyFeature());
        entity.setUpdatedAt(new Date());
        
        repository.save(entity);
        
        settingsDTO.setDoctorId(doctorId);
        settingsDTO.setUpdatedAt(entity.getUpdatedAt());
        return settingsDTO;
    }
}
