package com.heal.doctor.controllers;

import com.heal.doctor.dto.SupportTicketRequestDTO;
import com.heal.doctor.dto.SupportTicketResponseDTO;
import com.heal.doctor.services.ISupportTicketService;
import com.heal.doctor.utils.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/support")
@RequiredArgsConstructor
public class SupportTicketController {

    private static final Logger logger = LoggerFactory.getLogger(SupportTicketController.class);
    private final ISupportTicketService supportTicketService;

    @PostMapping("/tickets")
    public ResponseEntity<ApiResponse<SupportTicketResponseDTO>> createSupportTicket(
            @Valid @RequestBody SupportTicketRequestDTO requestDTO) {
        logger.info("Received support ticket creation request: category: {}, subject: {}", 
                requestDTO.getCategory(), requestDTO.getSubject());
        
        SupportTicketResponseDTO ticket = supportTicketService.createSupportTicket(requestDTO);
        
        return ResponseEntity.ok(ApiResponse.<SupportTicketResponseDTO>builder()
                .success(true)
                .message("Support ticket created successfully")
                .data(ticket)
                .build());
    }

    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<ApiResponse<SupportTicketResponseDTO>> getSupportTicket(
            @PathVariable String ticketId) {
        logger.debug("Fetching support ticket: ticketId: {}", ticketId);
        
        SupportTicketResponseDTO ticket = supportTicketService.getSupportTicketById(ticketId);
        
        return ResponseEntity.ok(ApiResponse.<SupportTicketResponseDTO>builder()
                .success(true)
                .message("Support ticket fetched successfully")
                .data(ticket)
                .build());
    }

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<List<SupportTicketResponseDTO>>> getAllSupportTickets(
            @RequestParam(required = false) String status) {
        logger.debug("Fetching support tickets: status: {}", status);
        
        List<SupportTicketResponseDTO> tickets;
        if (status != null && !status.isEmpty()) {
            tickets = supportTicketService.getSupportTicketsByStatus(status);
        } else {
            tickets = supportTicketService.getAllSupportTicketsForCurrentDoctor();
        }
        
        return ResponseEntity.ok(ApiResponse.<List<SupportTicketResponseDTO>>builder()
                .success(true)
                .message("Support tickets fetched successfully")
                .data(tickets)
                .build());
    }
}

