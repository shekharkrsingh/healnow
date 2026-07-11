package com.heal.doctor.services;

import com.heal.doctor.dto.AdminUpdateTicketDTO;
import com.heal.doctor.dto.SupportTicketResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IAdminSupportTicketService {
    List<SupportTicketResponseDTO> getAllTickets();
    Page<SupportTicketResponseDTO> getAllTicketsPaginated(Pageable pageable, String search, String status);
    SupportTicketResponseDTO updateTicketStatus(String ticketId, AdminUpdateTicketDTO updateDTO);
}
