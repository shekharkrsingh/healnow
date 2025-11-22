package com.heal.doctor.repositories;

import com.heal.doctor.models.SupportTicketEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends MongoRepository<SupportTicketEntity, String> {
    Optional<SupportTicketEntity> findByTicketId(String ticketId);
    List<SupportTicketEntity> findByDoctorIdOrderByCreatedAtDesc(String doctorId);
    List<SupportTicketEntity> findByDoctorIdAndStatusOrderByCreatedAtDesc(String doctorId, String status);
    List<SupportTicketEntity> findByStatusOrderByCreatedAtDesc(String status);
    boolean existsByTicketId(String ticketId);
}

