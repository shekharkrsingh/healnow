package com.heal.doctor.utils;

import com.heal.doctor.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class AppointmentId {

    private static final String DOCTOR_ID_PREFIX = "DOC-";
    private static final String APPOINTMENT_DATE_FORMAT = "yyyyMMdd-HHmmss";
    private static final int RANDOM_SUFFIX_MIN = 100;
    private static final int RANDOM_SUFFIX_MAX = 900;
    private static final int APPOINTMENT_ID_PARTS_COUNT = 5;

    public static String generateAppointmentId(String doctorId) {
        String doctorIdWithoutPrefix = doctorId.replace(DOCTOR_ID_PREFIX, "");

        long timestamp = System.currentTimeMillis();
        Date date = new Date(timestamp);

        SimpleDateFormat sdf = new SimpleDateFormat(APPOINTMENT_DATE_FORMAT);
        String formattedDate = sdf.format(date);

        SecureRandom random = new SecureRandom();
        int randomSuffix = RANDOM_SUFFIX_MIN + random.nextInt(RANDOM_SUFFIX_MAX);

        return doctorIdWithoutPrefix + "-" + formattedDate + "-" + randomSuffix;
    }

    public static String[] retrieveAppointmentDetails(String appointmentId) {
        String[] parts = appointmentId.split("-");

        if (parts.length == APPOINTMENT_ID_PARTS_COUNT) {
            String doctorId = parts[0] + "-" + parts[1] + "-" + parts[2];
            String appointmentDate = parts[3] + " " + parts[4];
            return new String[]{doctorId, appointmentDate};
        } else {
            throw new BadRequestException("Invalid appointment ID format");
        }
    }
}
