package com.heal.doctor.websocket;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    @MessageMapping("/appointment.update")
    @SendTo("/topic/appointments")
    public String handleAppointmentUpdate(String message) {
        return message;
    }
}