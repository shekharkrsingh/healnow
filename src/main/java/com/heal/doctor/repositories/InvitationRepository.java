package com.heal.doctor.repositories;

import com.heal.doctor.models.InvitationEntity;
import com.heal.doctor.models.enums.InvitationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends MongoRepository<InvitationEntity, String> {
    Optional<InvitationEntity> findByInvitationToken(String invitationToken);
    Optional<InvitationEntity> findByInvitationId(String invitationId);
    List<InvitationEntity> findByEmailAndStatus(String email, InvitationStatus status);
    List<InvitationEntity> findByEmailAndStatusAndDoctorIdAndExpiresAtGreaterThan(
            String email,
            InvitationStatus status,
            String doctorId,
            Date currentTime
    );
    List<InvitationEntity> findByDoctorId(String doctorId);
    List<InvitationEntity> findByEmailAndDoctorId(String email, String doctorId);
}
