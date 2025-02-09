package com.heal.doctor.services;



import com.heal.doctor.dto.AppointmentDTO;
import com.heal.doctor.dto.AppointmentRequestDTO;
import com.heal.doctor.models.enums.AppointmentStatus;

import java.util.Date;
import java.util.List;

public interface IAppointmentService {
    AppointmentDTO bookAppointment(AppointmentRequestDTO requestDTO);
    AppointmentDTO getAppointmentById(String appointmentId);

//    List<AppointmentDTO> getAppointmentsByDoctorAndDate(Date date);

    AppointmentDTO updateAppointmentStatus(String appointmentId, AppointmentStatus status);
    AppointmentDTO updatePaymentStatus(String appointmentId, Boolean paymentStatus);
    AppointmentDTO updateTreatedStatus(String appointmentId, Boolean treatedStatus);
    AppointmentDTO updateAvailableAtClinic(String appointmentId, Boolean availableAtClinicStatus);
}
