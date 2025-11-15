package com.heal.doctor.utils;

import com.heal.doctor.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class AppointmentId {

    public static String generateAppointmentId(String doctorId) {
        String doctorIdWithoutPrefix = doctorId.replace("DOC-", "");

        long timestamp = System.currentTimeMillis();
        Date date = new Date(timestamp);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String formattedDate = sdf.format(date);

        SecureRandom random = new SecureRandom();
        int randomSuffix = 100 + random.nextInt(900);

        return doctorIdWithoutPrefix + "-" + formattedDate + "-" + randomSuffix;
    }

    public static String[] retrieveAppointmentDetails(String appointmentId) {
        String[] parts = appointmentId.split("-");

        if (parts.length == 5) {
            String doctorId = parts[0] + "-" + parts[1] + "-" + parts[2];
            String appointmentDate = parts[3] + " " + parts[4];
            return new String[]{doctorId, appointmentDate};
        } else {
            throw new BadRequestException("Invalid appointment ID format");
        }
    }
}
