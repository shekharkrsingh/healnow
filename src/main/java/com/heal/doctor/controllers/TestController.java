package com.heal.doctor.controller;

import com.heal.doctor.dto.AppointmentDTO;
import com.heal.doctor.websocket.WebSocketController;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final WebSocketController webSocketController;

    public TestController(WebSocketController webSocketController) {
        this.webSocketController = webSocketController;
    }

    @PostMapping("/send-test/{doctorId}")
    public String sendTestMessage(@PathVariable String doctorId) {
        try {
            AppointmentDTO appointment = new AppointmentDTO();
            appointment.setAppointmentId("dfdfdf");
            appointment.setPatientName("Test Patient");
//            appointment.setAppointmentDate("LocalDateTime.now().toString()");
//            appointment.setStatus("SCHEDULED");
//            appointment.setAction("TEST_MESSAGE");

            webSocketController.sendAppointmentUpdate(doctorId, appointment);
            return "Test message sent to doctor: " + doctorId;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}