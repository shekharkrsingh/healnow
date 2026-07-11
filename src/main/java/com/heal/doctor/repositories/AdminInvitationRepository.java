package com.heal.doctor.repositories;

import com.heal.doctor.models.AdminInvitationEntity;
import com.heal.doctor.models.enums.InvitationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminInvitationRepository extends MongoRepository<AdminInvitationEntity, String> {
    Optional<AdminInvitationEntity> findByInvitationToken(String invitationToken);
    Optional<AdminInvitationEntity> findByInvitationId(String invitationId);
    List<AdminInvitationEntity> findByEmailAndStatus(String email, InvitationStatus status);
}
