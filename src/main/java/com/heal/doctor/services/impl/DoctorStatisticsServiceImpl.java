package com.heal.doctor.services.impl;

import com.heal.doctor.dto.DoctorStatisticsDTO;
import com.heal.doctor.models.DailyTreatedPatients;
import com.heal.doctor.repositories.DoctorStatisticsRepository;
import com.heal.doctor.services.IDoctorStatisticsService;
import com.heal.doctor.utils.CurrentUserName;
import com.heal.doctor.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class DoctorStatisticsServiceImpl implements IDoctorStatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(DoctorStatisticsServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int DAYS_BACK_WEEK = 7;

    private final DoctorStatisticsRepository statisticsRepository;
    private final Executor taskExecutor;

    public DoctorStatisticsServiceImpl(DoctorStatisticsRepository statisticsRepository,
                                      @Qualifier("emailTaskExecutor") Executor taskExecutor) {
        this.statisticsRepository = statisticsRepository;
        this.taskExecutor = taskExecutor;
    }

    public DoctorStatisticsDTO fetchStatistics() {
        logger.debug("Fetching statistics for doctor");
        
        Date[] todayDates = DateUtils.getStartAndEndOfDay(new Date());
        Date startOfDay = todayDates[0];
        Date endOfDay = todayDates[1];
        
        Date[] weekDates = getStartAndEndOfLastWeek();
        Date startOfWeek = weekDates[0];
        Date endOfYesterday = weekDates[1];

        String doctorId = CurrentUserName.getCurrentDoctorId();

        CompletableFuture<Integer> totalAppointmentsFuture = CompletableFuture.supplyAsync(
                () -> Objects.requireNonNullElse(statisticsRepository.getTotalAppointmentsToday(startOfDay, endOfDay, doctorId), 0),
                taskExecutor);
        
        CompletableFuture<Integer> untreatedNotAvailableFuture = CompletableFuture.supplyAsync(
                () -> Objects.requireNonNullElse(statisticsRepository.getTotalUntreatedAppointmentsTodayAndNotAvailable(startOfDay, endOfDay, doctorId), 0),
                taskExecutor);
        
        CompletableFuture<Integer> treatedAppointmentsFuture = CompletableFuture.supplyAsync(
                () -> Objects.requireNonNullElse(statisticsRepository.getTotalTreatedAppointmentsToday(startOfDay, endOfDay, doctorId), 0),
                taskExecutor);
        
        CompletableFuture<Integer> availableAtClinicFuture = CompletableFuture.supplyAsync(
                () -> Objects.requireNonNullElse(statisticsRepository.getTotalAvailableAtClinicToday(startOfDay, endOfDay, doctorId), 0),
                taskExecutor);
        
        CompletableFuture<List<DailyTreatedPatients>> dailyTreatedFuture = CompletableFuture.supplyAsync(
                () -> getProcessedDailyTreatedPatients(startOfWeek, endOfYesterday, doctorId),
                taskExecutor);
        
        CompletableFuture<DoctorStatisticsRepository.LastActiveDayStats> lastActiveDayStatsFuture = CompletableFuture.supplyAsync(
                () -> statisticsRepository.getLastActiveDayStats(doctorId, startOfWeek, endOfYesterday).orElse(null),
                taskExecutor);

        CompletableFuture.allOf(totalAppointmentsFuture, untreatedNotAvailableFuture, treatedAppointmentsFuture,
                availableAtClinicFuture, dailyTreatedFuture, lastActiveDayStatsFuture).join();

        Integer totalAppointmentsToday = totalAppointmentsFuture.join();
        Integer totalUntreatedAppointmentsTodayAndNotAvailable = untreatedNotAvailableFuture.join();
        Integer totalTreatedAppointmentsToday = treatedAppointmentsFuture.join();
        Integer totalAvailableAtClinic = availableAtClinicFuture.join();
        List<DailyTreatedPatients> dailyTreatedPatientsLastWeek = dailyTreatedFuture.join();
        DoctorStatisticsRepository.LastActiveDayStats lastActiveDayStats = lastActiveDayStatsFuture.join();
        Integer lastActiveDayAppointments = lastActiveDayStats != null && lastActiveDayStats.getTotalCount() != null 
                ? lastActiveDayStats.getTotalCount() : 0;
        Integer lastActiveDayTreatedAppointments = lastActiveDayStats != null && lastActiveDayStats.getTreatedCount() != null 
                ? lastActiveDayStats.getTreatedCount() : 0;

        DoctorStatisticsDTO doctorStatisticsDTO = new DoctorStatisticsDTO();
        doctorStatisticsDTO.setTotalAppointment(totalAppointmentsToday);
        doctorStatisticsDTO.setTotalUntreatedAppointmentAndNotAvailable(totalUntreatedAppointmentsTodayAndNotAvailable);
        doctorStatisticsDTO.setTotalTreatedAppointment(totalTreatedAppointmentsToday);
        doctorStatisticsDTO.setTotalAvailableAtClinic(totalAvailableAtClinic);
        doctorStatisticsDTO.setLastWeekTreatedData(dailyTreatedPatientsLastWeek);
        doctorStatisticsDTO.setLastActiveDayAppointments(lastActiveDayAppointments);
        doctorStatisticsDTO.setLastActiveDayTreatedAppointments(lastActiveDayTreatedAppointments);
        
        double percentage = 0.0;
        if (lastActiveDayAppointments != null && lastActiveDayAppointments > 0 && lastActiveDayTreatedAppointments != null) {
            percentage = ((double) lastActiveDayTreatedAppointments / lastActiveDayAppointments) * 100.0;
        }
        doctorStatisticsDTO.setLastActiveDayPercentageTreatedAppointments(percentage);
        
        logger.debug("Last active day statistics - Total: {}, Treated: {}, Percentage: {}%", 
                lastActiveDayAppointments, lastActiveDayTreatedAppointments, percentage);

        logger.debug("Statistics fetched successfully: totalAppointments: {}, treatedAppointments: {}",
                doctorStatisticsDTO.getTotalAppointment(), doctorStatisticsDTO.getTotalTreatedAppointment());
        
        return doctorStatisticsDTO;
    }

    private Date[] getStartAndEndOfLastWeek() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(DAYS_BACK_WEEK);
        LocalDate endOfYesterday = today.minusDays(1);
        
        Date[] startDates = DateUtils.getStartAndEndOfDay(java.util.Date.from(startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        Date[] endDates = DateUtils.getStartAndEndOfDay(java.util.Date.from(endOfYesterday.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        
        return new Date[]{startDates[0], endDates[1]};
    }

    private List<DailyTreatedPatients> getProcessedDailyTreatedPatients(Date startOfWeek, Date endOfYesterday, String doctorId) {
        List<DailyTreatedPatients> rawData = statisticsRepository.getDailyTreatedPatientsLastWeek(startOfWeek, endOfYesterday, doctorId);

        Map<String, Integer> treatedDataMap = rawData.parallelStream()
                .collect(Collectors.toConcurrentMap(DailyTreatedPatients::getDate, DailyTreatedPatients::getCount));

        List<DailyTreatedPatients> finalList = new ArrayList<>();
        LocalDate startDate = startOfWeek.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endDate = endOfYesterday.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            String dateStr = currentDate.format(DATE_FORMATTER);
            int count = treatedDataMap.getOrDefault(dateStr, 0);
            finalList.add(new DailyTreatedPatients(dateStr, count));
            currentDate = currentDate.plusDays(1);
        }

        return finalList;
    }
}
