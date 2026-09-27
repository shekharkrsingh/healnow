package com.heal.doctor.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility class for date operations.
 * All day-boundary calculations are performed in IST (Asia/Kolkata, UTC+05:30)
 * because this application is India-specific and MongoDB stores dates in UTC.
 */
@Component
public class DateUtils {

    /** Canonical IST zone used throughout the application. */
    public static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    /** Canonical IST TimeZone used throughout the application. */
    public static final TimeZone IST_TIMEZONE = TimeZone.getTimeZone("Asia/Kolkata");

    /**
     * Returns start (00:00:00.000 IST) and end (23:59:59.999999999 IST) of the
     * given date string as UTC-based {@link Date} objects suitable for MongoDB queries.
     */
    public static Date[] getStartAndEndOfDay(String dateString) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate localDate = LocalDate.parse(dateString, formatter);
            LocalDateTime startOfDay = localDate.atStartOfDay();
            LocalDateTime endOfDay = localDate.atTime(23, 59, 59, 999999999);
            // Explicitly attach IST zone before converting to Instant so the correct
            // UTC epoch millis are produced regardless of the JVM's default timezone.
            Date startOfDayDate = Date.from(startOfDay.atZone(IST).toInstant());
            Date endOfDayDate   = Date.from(endOfDay.atZone(IST).toInstant());
            return new Date[]{startOfDayDate, endOfDayDate};
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Please use 'yyyy-MM-dd'.", e);
        }
    }

    /**
     * Returns start and end of the day (in IST) for the given {@link Date} instant.
     */
    public static Date[] getStartAndEndOfDay(Date date) {
        try {
            LocalDate localDate = date.toInstant().atZone(IST).toLocalDate();
            LocalDateTime startOfDay = localDate.atStartOfDay();
            LocalDateTime endOfDay = localDate.atTime(23, 59, 59, 999999999);
            Date startOfDayDate = Date.from(startOfDay.atZone(IST).toInstant());
            Date endOfDayDate   = Date.from(endOfDay.atZone(IST).toInstant());
            return new Date[]{startOfDayDate, endOfDayDate};
        } catch (Exception e) {
            throw new IllegalArgumentException("Error processing date. Please ensure the date is valid.", e);
        }
    }

    /**
     * Parses a yyyy-MM-dd string to a {@link Date} representing midnight IST.
     */
    public static Date parseToDate(String dateString) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate localDate = LocalDate.parse(dateString, formatter);
            return Date.from(localDate.atStartOfDay(IST).toInstant());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Please use 'yyyy-MM-dd'.", e);
        }
    }

    /**
     * Returns today's date in IST.
     */
    public static LocalDate todayIST() {
        return LocalDate.now(IST);
    }
}

