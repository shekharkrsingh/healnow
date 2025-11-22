package com.heal.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketResponseDTO {
    private String ticketId;
    private String doctorId;
    private String doctorEmail;
    private String subject;
    private String message;
    private String category;
    private String status;
    private String priority;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant resolvedAt;
    private String adminResponse;
    private String assignedTo;
}

