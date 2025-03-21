package com.heal.doctor.services.impl;

import com.heal.doctor.dto.DoctorStatisticsDTO;
import com.heal.doctor.models.DailyTreatedPatients;
import com.heal.doctor.repositories.DoctorStatisticsRepository;
import com.heal.doctor.services.IDoctorStatisticsService;
import com.heal.doctor.utils.CurrentUserName;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorStatisticsServiceImpl implements IDoctorStatisticsService {

    private final DoctorStatisticsRepository statisticsRepository;

    public DoctorStatisticsServiceImpl(DoctorStatisticsRepository statisticsRepository) {
        this.statisticsRepository = statisticsRepository;
    }

    public DoctorStatisticsDTO fetchStatistics() {
        Date startOfDay = getStartOfDay();
        Date endOfDay = getEndOfDay();
        Date startOfWeek = getStartOfLast7Days();
        Date endOfYesterday = getEndOfYesterday();


        String doctorId= CurrentUserName.getCurrentDoctorId();

        Integer totalAppointmentsToday = Objects.requireNonNullElse(statisticsRepository.getTotalAppointmentsToday(startOfDay, endOfDay, doctorId), 0);
        Integer totalUntreatedAppointmentsToday = Objects.requireNonNullElse(statisticsRepository.getTotalUntreatedAppointmentsToday(startOfDay, endOfDay, doctorId), 0);
        Integer totalTreatedAppointmentsToday = Objects.requireNonNullElse(statisticsRepository.getTotalTreatedAppointmentsToday(startOfDay, endOfDay, doctorId), 0);
        Integer totalAvailableAtClinic = Objects.requireNonNullElse(statisticsRepository.getTotalAvailableAtClinicToday(startOfDay, endOfDay, doctorId), 0);
        List<DailyTreatedPatients> dailyTreatedPatientsLastWeek = getProcessedDailyTreatedPatients(startOfWeek, endOfYesterday, doctorId);


        DoctorStatisticsDTO doctorStatisticsDTO= new DoctorStatisticsDTO();
        doctorStatisticsDTO.setTotalAppointment(totalAppointmentsToday);
        doctorStatisticsDTO.setTotalUntreatedAppointment(totalUntreatedAppointmentsToday);
        doctorStatisticsDTO.setTotalTreatedAppointment(totalTreatedAppointmentsToday);
        doctorStatisticsDTO.setTotalAvailableAtClinic(totalAvailableAtClinic);
        doctorStatisticsDTO.setLastWeekTreatedData(dailyTreatedPatientsLastWeek);
        return doctorStatisticsDTO;
    }

    private Date getStartOfDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date getEndOfDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    private Date getStartOfLast7Days() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -7); // 7 days ago
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date getEndOfYesterday() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1); // Yesterday
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    private List<DailyTreatedPatients> getProcessedDailyTreatedPatients(Date startOfWeek, Date endOfYesterday, String doctorId) {
        List<DailyTreatedPatients> rawData = statisticsRepository.getDailyTreatedPatientsLastWeek(startOfWeek, endOfYesterday, doctorId);

        Map<String, Integer> treatedDataMap = rawData.stream()
                .collect(Collectors.toMap(DailyTreatedPatients::getDate, DailyTreatedPatients::getCount));

        List<DailyTreatedPatients> finalList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startOfWeek);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        while (!calendar.getTime().after(endOfYesterday)) {
            String dateStr = sdf.format(calendar.getTime());
            int count = treatedDataMap.getOrDefault(dateStr, 0); // Default to 0 if missing
            finalList.add(new DailyTreatedPatients(dateStr, count));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return finalList;
    }
}
