package com.heal.doctor.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "support_tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndexes({
    @CompoundIndex(name = "doctor_status_created_idx", def = "{'doctorId': 1, 'status': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "doctor_category_idx", def = "{'doctorId': 1, 'category': 1}"),
    @CompoundIndex(name = "ticket_id_idx", def = "{'ticketId': 1}")
})
public class SupportTicketEntity {

    @Id
    private String id;

    @Indexed(unique = true, name = "ticket_id_unique_idx")
    @NotBlank(message = "Ticket ID is required")
    @Size(max = 50, message = "Ticket ID must not exceed 50 characters")
    private String ticketId;

    @Indexed(name = "doctor_id_idx")
    @NotBlank(message = "Doctor ID is required")
    @Size(max = 50, message = "Doctor ID must not exceed 50 characters")
    private String doctorId;

    @NotBlank(message = "Doctor email is required")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String doctorEmail;

    @NotBlank(message = "Subject is required")
    @Size(min = 3, max = 200, message = "Subject must be between 3 and 200 characters")
    private String subject;

    @NotBlank(message = "Message is required")
    @Size(min = 10, max = 5000, message = "Message must be between 10 and 5000 characters")
    private String message;

    @NotNull(message = "Category is required")
    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    @Builder.Default
    private String status = "OPEN";

    @Builder.Default
    private String priority = "NORMAL";

    @CreatedDate
    private Instant createdAt;

    private Instant updatedAt;

    private Instant resolvedAt;

    @Size(max = 2000, message = "Admin response must not exceed 2000 characters")
    private String adminResponse;

    @Size(max = 50, message = "Assigned to must not exceed 50 characters")
    private String assignedTo;
}

