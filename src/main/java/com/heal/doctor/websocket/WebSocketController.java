package com.heal.doctor.websocket;

import com.heal.doctor.dto.AppointmentDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendAppointmentUpdate(String doctorId, AppointmentDTO appointment) {
        try {
            System.out.println("📢 Sending WebSocket message to doctor: " + doctorId);
            System.out.println("📦 Message content: " + appointment.toString());

            // Method 1: User-specific messaging (Primary)
            messagingTemplate.convertAndSendToUser(doctorId, "/queue/appointments", appointment);

            // Method 2: Also send to topic for testing
            messagingTemplate.convertAndSend("/topic/appointments", appointment);

            System.out.println("✅ WebSocket messages sent successfully");

        } catch (Exception e) {
            System.out.println("❌ Error sending WebSocket message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}