package com.heal.doctor.entity.services;

import com.heal.doctor.entity.dto.DataSharingPolicyUpdateRequest;
import com.heal.doctor.entity.dto.DoctorSharedProfileDTO;
import com.heal.doctor.entity.dto.EntitySharedProfileDTO;
import com.heal.doctor.entity.models.DataSharingPolicy;
import com.heal.doctor.entity.models.HealthcareEntity;
import com.heal.doctor.models.DoctorEntity;

public interface IDataSharingService {

    DoctorSharedProfileDTO filterDoctorProfile(DoctorEntity doctor, DataSharingPolicy policy);

    EntitySharedProfileDTO filterEntityProfile(HealthcareEntity entity, DataSharingPolicy policy);

    DataSharingPolicy createDefaultPolicy();

    DataSharingPolicy updatePolicy(String affiliationId, DataSharingPolicyUpdateRequest request, String actorUserId);
}
