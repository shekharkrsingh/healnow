package com.heal.doctor.entity.services.impl;

import com.heal.doctor.entity.dto.DataSharingPolicyUpdateRequest;
import com.heal.doctor.entity.dto.DoctorSharedProfileDTO;
import com.heal.doctor.entity.dto.EntitySharedProfileDTO;
import com.heal.doctor.entity.models.DataSharingPolicy;
import com.heal.doctor.entity.models.HealthcareEntity;
import com.heal.doctor.entity.repositories.EntityAffiliationRepository;
import com.heal.doctor.entity.services.IDataSharingService;
import com.heal.doctor.exception.ResourceNotFoundException;
import com.heal.doctor.models.DoctorEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DataSharingServiceImpl implements IDataSharingService {

    private final EntityAffiliationRepository affiliationRepository;

    @Override
    public DoctorSharedProfileDTO filterDoctorProfile(DoctorEntity doctor, DataSharingPolicy policy) {
        Map<String, Object> filteredFields = new HashMap<>();
        Set<String> allowedFields = policy.getDoctorSharedFields();

        if (allowedFields == null || allowedFields.isEmpty()) {
            return DoctorSharedProfileDTO.builder().fields(filteredFields).build();
        }

        for (String fieldName : allowedFields) {
            try {
                Field field = DoctorEntity.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(doctor);
                if (value != null) {
                    filteredFields.put(fieldName, value);
                }
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
        }

        return DoctorSharedProfileDTO.builder().fields(filteredFields).build();
    }

    @Override
    public EntitySharedProfileDTO filterEntityProfile(HealthcareEntity entity, DataSharingPolicy policy) {
        Map<String, Object> filteredFields = new HashMap<>();
        Set<String> allowedFields = policy.getEntitySharedFields();

        if (allowedFields == null || allowedFields.isEmpty()) {
            return EntitySharedProfileDTO.builder().fields(filteredFields).build();
        }

        for (String fieldName : allowedFields) {
            try {
                Field field = HealthcareEntity.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(entity);
                if (value != null) {
                    filteredFields.put(fieldName, value);
                }
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
        }

        return EntitySharedProfileDTO.builder().fields(filteredFields).build();
    }

    @Override
    public DataSharingPolicy createDefaultPolicy() {
        return DataSharingPolicy.builder()
                .version(1)
                .doctorSharedFields(Set.of("firstName", "lastName", "specialization",
                        "profilePicture", "licenseNumber", "verificationStatus"))
                .entitySharedFields(Set.of("name", "address", "city", "phoneNumber",
                        "departments", "logoUrl"))
                .agreedAt(new Date())
                .build();
    }

    @Override
    public DataSharingPolicy updatePolicy(String affiliationId, DataSharingPolicyUpdateRequest request, String actorUserId) {
        var aff = affiliationRepository.findByAffiliationId(affiliationId)
                .orElseThrow(() -> new ResourceNotFoundException("EntityAffiliation", affiliationId));

        DataSharingPolicy existing = aff.getDataSharingPolicy();
        int newVersion = existing != null ? existing.getVersion() + 1 : 1;

        DataSharingPolicy updated = DataSharingPolicy.builder()
                .version(newVersion)
                .doctorSharedFields(request.getDoctorSharedFields())
                .entitySharedFields(request.getEntitySharedFields())
                .featureFlags(request.getFeatureFlags())
                .agreedAt(new Date())
                .agreedByEntityUserId(actorUserId)
                .build();

        aff.setDataSharingPolicy(updated);
        aff.setUpdatedAt(new Date());
        affiliationRepository.save(aff);

        return updated;
    }
}
