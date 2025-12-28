package com.heal.doctor.services;

import com.heal.doctor.models.AppointmentEntity;
import java.util.concurrent.CompletableFuture;

public interface IAppointmentConfirmationService {
    CompletableFuture<Void> sendConfirmationEmail(AppointmentEntity appointment);
}
