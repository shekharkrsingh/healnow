package com.heal.doctor.services;

import com.heal.doctor.models.AppointmentEntity;
import com.heal.doctor.models.DoctorEntity;

import java.util.List;

public interface IDoctorReports {

     byte[] generateDoctorReport(
            String fromDate,
            String toDate
    );
}
