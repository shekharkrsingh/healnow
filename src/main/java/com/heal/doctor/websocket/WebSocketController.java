package com.heal.doctor.websocket;

import com.heal.doctor.dto.AppointmentDTO;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Method to send updates to a specific doctor
    public void sendAppointmentUpdate(String doctorId, AppointmentDTO appointment) {
        String destination = "/topic/appointments/" + doctorId;
        messagingTemplate.convertAndSend(destination, appointment);
    }

}

