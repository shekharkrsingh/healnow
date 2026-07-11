package com.heal.doctor.controllers;

import com.heal.doctor.dto.AdminUpdateTicketDTO;
import com.heal.doctor.dto.SupportTicketResponseDTO;
import com.heal.doctor.services.IAdminSupportTicketService;
import com.heal.doctor.utils.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/support/tickets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSupportTicketController {

    private final IAdminSupportTicketService adminSupportTicketService;

    @GetMapping
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<SupportTicketResponseDTO>>> getAllTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        
        org.springframework.data.domain.Sort.Direction sortDirection = org.springframework.data.domain.Sort.Direction.fromString(direction);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
            page, size, org.springframework.data.domain.Sort.by(sortDirection, sortBy)
        );
        org.springframework.data.domain.Page<SupportTicketResponseDTO> tickets = adminSupportTicketService.getAllTicketsPaginated(pageable, search, status);
        
        return ResponseEntity.ok(ApiResponse.<org.springframework.data.domain.Page<SupportTicketResponseDTO>>builder()
                .success(true)
                .message("Tickets fetched successfully")
                .data(tickets)
                .build());
    }



    @PutMapping("/{ticketId}")
    public ResponseEntity<ApiResponse<SupportTicketResponseDTO>> updateTicketStatus(
            @PathVariable String ticketId,
            @Valid @RequestBody AdminUpdateTicketDTO updateDTO) {
        SupportTicketResponseDTO updatedTicket = adminSupportTicketService.updateTicketStatus(ticketId, updateDTO);
        return ResponseEntity.ok(ApiResponse.<SupportTicketResponseDTO>builder()
                .success(true)
                .message("Ticket updated successfully")
                .data(updatedTicket)
                .build());
    }
}
