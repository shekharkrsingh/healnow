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

        CompletableFuture<DoctorStatisticsRepository.StatisticsResult> todayStatsFuture = CompletableFuture.supplyAsync(
                () -> statisticsRepository.getTodayStatisticsOptimized(startOfDay, endOfDay, doctorId),
                taskExecutor);
        
        CompletableFuture<List<DailyTreatedPatients>> dailyTreatedFuture = CompletableFuture.supplyAsync(
                () -> getProcessedDailyTreatedPatients(startOfWeek, endOfYesterday, doctorId),
                taskExecutor);
        
        CompletableFuture<Integer> lastActiveDayAppointmentsFuture = CompletableFuture.supplyAsync(
                () -> statisticsRepository.getLastActiveDayAppointments(doctorId, startOfWeek, endOfYesterday).orElse(0),
                taskExecutor);
        
        CompletableFuture<Integer> lastActiveDayTreatedFuture = CompletableFuture.supplyAsync(
                () -> statisticsRepository.getLastActiveDayTreatedAppointments(doctorId, startOfWeek, endOfYesterday).orElse(0),
                taskExecutor);

        CompletableFuture.allOf(todayStatsFuture, dailyTreatedFuture, lastActiveDayAppointmentsFuture, lastActiveDayTreatedFuture).join();

        DoctorStatisticsRepository.StatisticsResult todayStats = todayStatsFuture.join();
        List<DailyTreatedPatients> dailyTreatedPatientsLastWeek = dailyTreatedFuture.join();
        Integer lastActiveDayAppointments = lastActiveDayAppointmentsFuture.join();
        Integer lastActiveDayTreatedAppointments = lastActiveDayTreatedFuture.join();

        DoctorStatisticsDTO doctorStatisticsDTO = new DoctorStatisticsDTO();
        doctorStatisticsDTO.setTotalAppointment(Objects.requireNonNullElse(todayStats.getTotalAppointments(), 0));
        doctorStatisticsDTO.setTotalUntreatedAppointmentAndNotAvailable(Objects.requireNonNullElse(todayStats.getUntreatedNotAvailable(), 0));
        doctorStatisticsDTO.setTotalTreatedAppointment(Objects.requireNonNullElse(todayStats.getTreatedAppointments(), 0));
        doctorStatisticsDTO.setTotalAvailableAtClinic(Objects.requireNonNullElse(todayStats.getAvailableAtClinic(), 0));
        doctorStatisticsDTO.setLastWeekTreatedData(dailyTreatedPatientsLastWeek);
        doctorStatisticsDTO.setLastActiveDayAppointments(lastActiveDayAppointments);
        doctorStatisticsDTO.setLastActiveDayTreatedAppointments(lastActiveDayTreatedAppointments);
        
        double percentage = lastActiveDayAppointments > 0
                ? ((double)lastActiveDayTreatedAppointments / lastActiveDayAppointments) * 100
                : 0.0;
        doctorStatisticsDTO.setLastActiveDayPercentageTreatedAppointments(percentage);

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
