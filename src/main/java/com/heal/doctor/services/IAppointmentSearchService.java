package com.heal.doctor.services;

import com.heal.doctor.dto.AppointmentDTO;
import com.heal.doctor.dto.AppointmentSearchDTO;

import java.util.List;

public interface IAppointmentSearchService {
    List<AppointmentDTO> searchAppointment(AppointmentSearchDTO appointmentSearchDTO);
}
