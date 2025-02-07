package com.heal.doctor.services;



import com.heal.doctor.dto.AppointmentDTO;
import com.heal.doctor.models.enums.AppointmentStatus;

import java.util.List;

public interface IAppointmentService {
    AppointmentDTO bookAppointment(AppointmentDTO requestDTO);
    AppointmentDTO getAppointmentById(String appointmentId);
    List<AppointmentDTO> getAppointmentsByDoctorAndDate(String doctorId, String date);
    AppointmentDTO updateAppointmentStatus(String appointmentId, AppointmentStatus status);
    AppointmentDTO updatePaymentStatus(String appointmentId, Boolean paymentStatus);
    AppointmentDTO updateTreatedStatus(String appointmentId, Boolean treatedStatus);
    AppointmentDTO updateAvailableAtClinic(String appointmentId, Boolean availableAtClinicStatus);
    void cancelAppointment(String appointmentId);
}
