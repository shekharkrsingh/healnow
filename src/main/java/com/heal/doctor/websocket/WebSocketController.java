package com.heal.doctor.websocket;

import com.heal.doctor.dto.AppointmentDTO;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    @MessageMapping("/update-appointment")
    @SendTo("/topic/appointments")
    public AppointmentDTO updateAppointment(AppointmentDTO appointment) {
        return appointment;
    }
}

