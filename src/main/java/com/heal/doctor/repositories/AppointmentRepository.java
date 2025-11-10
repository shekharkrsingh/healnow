package com.heal.doctor.repositories;

import com.heal.doctor.models.AppointmentEntity;
import com.heal.doctor.models.enums.AppointmentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends MongoRepository<AppointmentEntity, String> {
    Optional<AppointmentEntity> findByAppointmentId(String appointmentId);

    List<AppointmentEntity> findByDoctorIdAndBookingDateTimeBetween(String doctorId, Date startDate, Date endDate);

    Boolean existsByDoctorIdAndPatientNameAndContactAndAppointmentDateTimeBetweenAndStatus(
            String doctorId,
            String patientName,
            String contact,
            Date startDate,
            Date endDate,
            AppointmentStatus status
    );


    List<AppointmentEntity> findByDoctorIdAndAppointmentDateTimeBetween(
            String doctorId, Date fromDate, Date toDate
    );

}
