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
            "{ $match: { doctorId: ?2, appointmentDateTime: { $gte: ?0, $lte: ?1 } } }",
            "{ $count: 'count' }"
    })
    Integer getTotalAppointmentsToday(Date startOfDay, Date endOfDay, String doctorId);

    @Aggregation(pipeline = {
            "{ $match: { doctorId: ?2, appointmentDateTime: { $gte: ?0, $lte: ?1 }, availableAtClinic: false, treated: false } }",
            "{ $count: 'count' }"
    })
    Integer getTotalUntreatedAppointmentsTodayAndNotAvailable(Date startOfDay, Date endOfDay, String doctorId);

    @Aggregation(pipeline = {
            "{ $match: { doctorId: ?2, appointmentDateTime: { $gte: ?0, $lte: ?1 }, treated: true } }",
            "{ $count: 'count' }"
    })
    Integer getTotalTreatedAppointmentsToday(Date startOfDay, Date endOfDay, String doctorId);

    @Aggregation(pipeline = {
            "{ $match: { doctorId: ?2, appointmentDateTime: { $gte: ?0, $lte: ?1 }, availableAtClinic: true, treated: false } }",
            "{ $count: 'count' }"
    })
    Integer getTotalAvailableAtClinicToday(Date startOfDay, Date endOfDay, String doctorId);

    @Aggregation(pipeline = {
            "{ $match: { doctorId: ?0, appointmentDateTime: { $gte: ?1, $lte: ?2 } } }",
            "{ $facet: {",
            "  totalAppointments: [{ $count: 'count' }],",
            "  treatedAppointments: [{ $match: { treated: true } }, { $count: 'count' }],",
            "  untreatedNotAvailable: [{ $match: { treated: false, availableAtClinic: false } }, { $count: 'count' }],",
            "  availableAtClinic: [{ $match: { treated: false, availableAtClinic: true } }, { $count: 'count' }]",
            "}}",
            "{ $project: {",
            "  totalAppointments: { $ifNull: [{ $arrayElemAt: ['$totalAppointments.count', 0] }, 0] },",
            "  treatedAppointments: { $ifNull: [{ $arrayElemAt: ['$treatedAppointments.count', 0] }, 0] },",
            "  untreatedNotAvailable: { $ifNull: [{ $arrayElemAt: ['$untreatedNotAvailable.count', 0] }, 0] },",
            "  availableAtClinic: { $ifNull: [{ $arrayElemAt: ['$availableAtClinic.count', 0] }, 0] }",
            "}}"
    })
    StatisticsResult getTodayStatisticsOptimized(Date startOfDay, Date endOfDay, String doctorId);

    interface StatisticsResult {
        Integer getTotalAppointments();
        Integer getTreatedAppointments();
        Integer getUntreatedNotAvailable();
        Integer getAvailableAtClinic();
    }

    @Aggregation(pipeline = {
            "{ $match: { doctorId: ?2, treatedDateTime: { $gte: ?0, $lte: ?1 }, treated: true } }",
            "{ $group: { _id: { $dateToString: { format: '%Y-%m-%d', date: '$treatedDateTime' } }, count: { $sum: 1 } } }",
            "{ $project: { _id: 0, date: '$_id', count: 1 } }",
            "{ $sort: { date: 1 } }"
    })
    List<DailyTreatedPatients> getDailyTreatedPatientsLastWeek(Date startOfWeek, Date endOfYesterday, String doctorId);

    @Aggregation(pipeline = {
            "{ $match: { doctorId: ?0, appointmentDateTime: { $gte: ?1, $lte: ?2 } } }",
            "{ $group: { _id: { $dateToString: { format: '%Y-%m-%d', date: '$appointmentDateTime' } }, totalCount: { $sum: 1 }, treatedCount: { $sum: { $cond: [{ $eq: ['$treated', true] }, 1, 0] } } } }",
            "{ $sort: { _id: -1 } }",
            "{ $limit: 1 }",
            "{ $project: { _id: 0, totalCount: { $ifNull: ['$totalCount', 0] }, treatedCount: { $ifNull: ['$treatedCount', 0] } } }"
    })
    Optional<LastActiveDayStats> getLastActiveDayStats(String doctorId, Date startOfLast7Days, Date endOfYesterday);

    interface LastActiveDayStats {
        Integer getTotalCount();
        Integer getTreatedCount();
    }


}
