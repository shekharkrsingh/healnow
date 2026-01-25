package com.heal.doctor.repositories;

import com.heal.doctor.models.AppointmentEntity;
import com.heal.doctor.models.enums.AppointmentStatus;
import com.heal.doctor.models.enums.AppointmentType;
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
    List<AppointmentEntity> findByEmail(String email);

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

    @Query("""
    {
      doctorId: ?0,
      $and: [
        { $or: [ { appointmentId: ?1 }, { $expr: { $eq: [ ?1, null ] } } ] },
        { $or: [ { patientName: { $regex: :#{#patientName == null ? '^$' : #patientName}, $options: 'i' } }, { $expr: { $eq: [ ?2, null ] } } ] },
        { $or: [ { contact: ?3 }, { $expr: { $eq: [ ?3, null ] } } ] },
        { $or: [ { email: { $regex: :#{#email == null ? '^$' : #email}, $options: 'i' } }, { $expr: { $eq: [ ?4, null ] } } ] },
        { $or: [ { appointmentDateTime: { $gte: ?5, $lte: ?6 } }, { $expr: { $eq: [ ?5, null ] } } ] },
        { $or: [ { bookingDateTime: { $gte: ?7, $lte: ?8 } }, { $expr: { $eq: [ ?7, null ] } } ] },
        { $or: [ { status: ?9 }, { $expr: { $eq: [ ?9, null ] } } ] },
        { $or: [ { appointmentType: ?10 }, { $expr: { $eq: [ ?10, null ] } } ] }
      ]
    }
    """)
    List<AppointmentEntity> searchAppointmentsByDoctorId(
            String doctorId,
            String appointmentId,
            @Param("patientName") String patientName,
            String contact,
            @Param("email") String email,
            Date appointmentDateStart,
            Date appointmentDateEnd,
            Date bookingDateStart,
            Date bookingDateEnd,
            AppointmentStatus status,
            AppointmentType appointmentType
    );


}
