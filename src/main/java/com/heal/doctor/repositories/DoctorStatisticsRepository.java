package com.heal.doctor.repositories;

import com.heal.doctor.models.AppointmentEntity;
import com.heal.doctor.models.DailyTreatedPatients;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorStatisticsRepository extends MongoRepository<AppointmentEntity, String> {

    @Aggregation(pipeline = {
            "{ $match: { doctorId: ?2, appointmentDateTime: { $gte: ?0, $lt: ?1 } } }",
            "{ $count: 'count' }"
    })
    Integer getTotalAppointmentsToday(Date startOfDay, Date endOfDay, String doctorId);

    @Aggregation(pipeline = {
            "{ $match: { doctorId: ?2, appointmentDateTime: { $gte: ?0, $lt: ?1 }, availableAtClinic: false, treated: false } }",
            "{ $count: 'count' }"
    })
    Integer getTotalUntreatedAppointmentsTodayAndNotAvailable(Date startOfDay, Date endOfDay, String doctorId);

    @Aggregation(pipeline = {
            "{ $match: { doctorId: ?2, appointmentDateTime: { $gte: ?0, $lt: ?1 }, treated: true } }",
            "{ $count: 'count' }"
    })
    Integer getTotalTreatedAppointmentsToday(Date startOfDay, Date endOfDay, String doctorId);

    @Aggregation(pipeline = {
            "{ $match: { doctorId: ?2, appointmentDateTime: { $gte: ?0, $lt: ?1 }, availableAtClinic: true, treated: false } }",
            "{ $count: 'count' }"
    })
    Integer getTotalAvailableAtClinicToday(Date startOfDay, Date endOfDay, String doctorId);

    @Aggregation(pipeline = {
            "{ $match: { doctorId: ?2, treatedDateTime: { $gte: ?0, $lt: ?1 }, treated: true } }",
            "{ $group: { _id: { $dateToString: { format: '%Y-%m-%d', date: '$treatedDateTime' } }, count: { $sum: 1 } } }",
            "{ $project: { _id: 0, date: '$_id', count: 1 } }",
            "{ $sort: { date: 1 } }"
    })
    List<DailyTreatedPatients> getDailyTreatedPatientsLastWeek(Date startOfWeek, Date endOfYesterday, String doctorId);

    @Aggregation(pipeline = {
            "{ $match: { doctorId: ?0, appointmentDateTime: { $gte: ?1, $lt: ?2 } } }",
            "{ $group: { _id: { $dateToString: { format: '%Y-%m-%d', date: '$appointmentDateTime' } }, count: { $sum: 1 } } }",
            "{ $sort: { _id: -1 } }", // Get the latest active day
            "{ $limit: 1 }",
            "{ $project: { _id: 0, count: 1 } }"
    })
    Optional<Integer> getLastActiveDayAppointments(String doctorId, Date startOfLast7Days, Date endOfYesterday);

    @Aggregation(pipeline = {
            "{ $match: { doctorId: ?0, treated: true, appointmentDateTime: { $gte: ?1, $lt: ?2 } } }",
            "{ $group: { _id: { $dateToString: { format: '%Y-%m-%d', date: '$appointmentDateTime' } }, count: { $sum: 1 } } }",
            "{ $sort: { _id: -1 } }",
            "{ $limit: 1 }",
            "{ $project: { _id: 0, count: 1 } }"
    })
    Optional<Integer> getLastActiveDayTreatedAppointments(String doctorId, Date startOfLast7Days, Date endOfYesterday);


}
