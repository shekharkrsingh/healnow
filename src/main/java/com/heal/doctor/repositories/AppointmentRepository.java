package com.heal.doctor.repositories;

import com.heal.doctor.models.AppointmentEntity;
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

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
            "FROM Appointment a " +
            "WHERE a.doctorId = :doctorId " +
            "AND a.patientName = :patientName " +
            "AND a.contact = :contact " +
            "AND a.appointmentDateTime BETWEEN :startDate AND :endDate " +
            "AND a.status = 'ACCEPTED'")
    boolean existsAcceptedAppointment(
            @Param("doctorId") String doctorId,
            @Param("patientName") String patientName,
            @Param("contact") String contact,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );

    List<AppointmentEntity> findByDoctorIdAndAppointmentDateTimeBetween(
            String doctorId, Date fromDate, Date toDate
    );

}
