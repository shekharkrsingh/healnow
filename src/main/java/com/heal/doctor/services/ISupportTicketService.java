package com.heal.doctor.services;

import com.heal.doctor.dto.SupportTicketRequestDTO;
import com.heal.doctor.dto.SupportTicketResponseDTO;

import java.util.List;

public interface ISupportTicketService {
    SupportTicketResponseDTO createSupportTicket(SupportTicketRequestDTO requestDTO);
    SupportTicketResponseDTO getSupportTicketById(String ticketId);
    List<SupportTicketResponseDTO> getAllSupportTicketsForCurrentDoctor();
    List<SupportTicketResponseDTO> getSupportTicketsByStatus(String status);
}

