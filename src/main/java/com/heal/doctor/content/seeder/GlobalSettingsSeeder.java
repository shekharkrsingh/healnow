package com.heal.doctor.content.seeder;

import com.heal.doctor.models.DefaultCollaboratorSettingsEntity;
import com.heal.doctor.models.DefaultDoctorSettingsEntity;
import com.heal.doctor.models.GlobalAppSettingsEntity;
import com.heal.doctor.repositories.DefaultCollaboratorSettingsRepository;
import com.heal.doctor.repositories.DefaultDoctorSettingsRepository;
import com.heal.doctor.repositories.GlobalAppSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class GlobalSettingsSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(GlobalSettingsSeeder.class);
    private final GlobalAppSettingsRepository globalAppSettingsRepository;
    private final DefaultDoctorSettingsRepository defaultDoctorSettingsRepository;
    private final DefaultCollaboratorSettingsRepository defaultCollaboratorSettingsRepository;

    @Override
    public void run(String... args) {
        if (globalAppSettingsRepository.findById(GlobalAppSettingsEntity.SINGLETON_ID).isEmpty()) {
            logger.info("Seeding initial Global App Settings");
            GlobalAppSettingsEntity defaultSettings = GlobalAppSettingsEntity.builder()
                    .id(GlobalAppSettingsEntity.SINGLETON_ID)
                    .maintenanceMode(false)
                    .allowNewRegistrations(true)
                    .contactEmail("support@healnow.com")
                    .updatedAt(new Date())
                    .build();
            globalAppSettingsRepository.save(defaultSettings);
            logger.info("Seeded initial Global App Settings successfully.");
        }

        if (defaultDoctorSettingsRepository.findById(DefaultDoctorSettingsEntity.SINGLETON_ID).isEmpty()) {
            logger.info("Seeding initial Default Doctor Settings");
            DefaultDoctorSettingsEntity defaultSettings = DefaultDoctorSettingsEntity.builder()
                    .id(DefaultDoctorSettingsEntity.SINGLETON_ID)
                    .publicBookingAllowed(true)
                    .enableEmergencyFeature(false)
                    .updatedAt(new Date())
                    .build();
            defaultDoctorSettingsRepository.save(defaultSettings);
            logger.info("Seeded initial Default Doctor Settings successfully.");
        }

        if (defaultCollaboratorSettingsRepository.findById(DefaultCollaboratorSettingsEntity.SINGLETON_ID).isEmpty()) {
            logger.info("Seeding initial Default Collaborator Settings");
            DefaultCollaboratorSettingsEntity defaultSettings = DefaultCollaboratorSettingsEntity.builder()
                    .id(DefaultCollaboratorSettingsEntity.SINGLETON_ID)
                    .enableNotifications(true)
                    .twoFactorAuthEnabled(false)
                    .updatedAt(new Date())
                    .build();
            defaultCollaboratorSettingsRepository.save(defaultSettings);
            logger.info("Seeded initial Default Collaborator Settings successfully.");
        }
    }
}
