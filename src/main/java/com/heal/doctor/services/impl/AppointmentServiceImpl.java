package com.heal.doctor.services.impl;

import com.heal.doctor.dto.AppointmentDTO;
import com.heal.doctor.models.enums.AppointmentStatus;
import com.heal.doctor.services.IAppointmentService;

import java.util.List;

public class AppointmentServiceImpl implements IAppointmentService {
    @Override
    public AppointmentDTO bookAppointment(AppointmentDTO requestDTO) {
        return null;
    }

    @Override
    public AppointmentDTO getAppointmentById(String appointmentId) {
        return null;
    }

    @Override
    public List<AppointmentDTO> getAppointmentsByDoctorAndDate(String doctorId, String date) {
        return List.of();
    }

    @Override
    public AppointmentDTO updateAppointmentStatus(String appointmentId, AppointmentStatus status) {
        return null;
    }

    @Override
    public AppointmentDTO updatePaymentStatus(String appointmentId, Boolean paymentStatus) {
        return null;
    }

    @Override
    public AppointmentDTO updateTreatedStatus(String appointmentId, Boolean treatedStatus) {
        return null;
    }

    @Override
    public AppointmentDTO updateAvailableAtClinic(String appointmentId, Boolean availableAtClinicStatus) {
        return null;
    }

    @Override
    public void cancelAppointment(String appointmentId) {

    }
}
