package com.heal.doctor.services.impl;

import com.heal.doctor.dto.DefaultDoctorSettingsDTO;
import com.heal.doctor.models.DefaultDoctorSettingsEntity;
import com.heal.doctor.repositories.DefaultDoctorSettingsRepository;
import com.heal.doctor.services.IDefaultDoctorSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultDoctorSettingsServiceImpl implements IDefaultDoctorSettingsService {

    private final DefaultDoctorSettingsRepository repository;

    @Override
    public DefaultDoctorSettingsDTO getSettings() {
        Optional<DefaultDoctorSettingsEntity> optionalSettings = repository.findById(DefaultDoctorSettingsEntity.SINGLETON_ID);
        
        if (optionalSettings.isEmpty()) {
            return DefaultDoctorSettingsDTO.builder()
                    .publicBookingAllowed(true)
                    .enableEmergencyFeature(false)
                    .build();
        }
        
        DefaultDoctorSettingsEntity entity = optionalSettings.get();
        return DefaultDoctorSettingsDTO.builder()
                .publicBookingAllowed(entity.isPublicBookingAllowed())
                .enableEmergencyFeature(entity.isEnableEmergencyFeature())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public DefaultDoctorSettingsDTO updateSettings(DefaultDoctorSettingsDTO settingsDTO) {
        DefaultDoctorSettingsEntity entity = repository.findById(DefaultDoctorSettingsEntity.SINGLETON_ID)
                .orElse(DefaultDoctorSettingsEntity.builder().id(DefaultDoctorSettingsEntity.SINGLETON_ID).build());
        
        entity.setPublicBookingAllowed(settingsDTO.isPublicBookingAllowed());
        entity.setEnableEmergencyFeature(settingsDTO.isEnableEmergencyFeature());
        entity.setUpdatedAt(new Date());
        
        repository.save(entity);
        
        settingsDTO.setUpdatedAt(entity.getUpdatedAt());
        return settingsDTO;
    }
}
