package com.heal.doctor.utils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Component
public class DateUtils {

    public static Date[] getStartAndEndOfDay(String dateString) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate localDate = LocalDate.parse(dateString, formatter);
            LocalDateTime startOfDay = localDate.atStartOfDay();
            LocalDateTime endOfDay = localDate.atTime(23, 59, 59, 999999999);
            Date startOfDayDate = java.sql.Timestamp.valueOf(startOfDay);
            Date endOfDayDate = java.sql.Timestamp.valueOf(endOfDay);
            return new Date[] {startOfDayDate, endOfDayDate};
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Please use 'yyyy-MM-dd'.", e);
        }
    }

    public static Date[] getStartAndEndOfDay(Date date) {
        try {
            LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDateTime startOfDay = localDate.atStartOfDay();
            LocalDateTime endOfDay = localDate.atTime(23, 59, 59, 999999999);
            Date startOfDayDate = java.util.Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant());
            Date endOfDayDate = java.util.Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant());
            return new Date[] {startOfDayDate, endOfDayDate};
        } catch (Exception e) {
            throw new IllegalArgumentException("Error processing date. Please ensure the date is valid.", e);
        }
    }

    public static Date parseToDate(String dateString) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate localDate = LocalDate.parse(dateString, formatter);
            return java.util.Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Please use 'yyyy-MM-dd'.", e);
        }
    }

}
