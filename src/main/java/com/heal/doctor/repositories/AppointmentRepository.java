package com.heal.doctor.repositories;

import com.heal.doctor.models.AppointmentEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends MongoRepository<AppointmentEntity, String> {
    Optional<AppointmentEntity> findByAppointmentId(String appointmentId);

    List<AppointmentEntity> findByDoctorIdAndAppointmentDateTime(String doctorId, Date appointmentDateTime);

    boolean existsByDoctorIdAndAppointmentDateTime(String doctorId, Date appointmentDateTime); // Change to appointmentDateTime
}
