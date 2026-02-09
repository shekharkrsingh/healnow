package com.heal.doctor.services.impl;

import com.heal.doctor.dto.AppointmentDTO;
import com.heal.doctor.dto.AppointmentSearchDTO;
import com.heal.doctor.exception.ResourceNotFoundException;
import com.heal.doctor.models.AppointmentEntity;
import com.heal.doctor.models.enums.AppointmentStatus;
import com.heal.doctor.repositories.AppointmentRepository;
import com.heal.doctor.services.IAppointmentSearchService;
import com.heal.doctor.utils.CurrentUserName;
import com.heal.doctor.utils.DateUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
@Service
public class AppointmentSearchService implements IAppointmentSearchService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentSearchService.class);

    private final AppointmentRepository appointmentRepository;
    private final ModelMapper modelMapper;

    public AppointmentSearchService(AppointmentRepository appointmentRepository,
                                    ModelMapper modelMapper) {
        this.appointmentRepository = appointmentRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentDTO> searchAppointment(AppointmentSearchDTO appointmentSearchDTO) {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        String userId = CurrentUserName.getCurrentUserId();

        logger.info("Initiating appointment search for DoctorID: [{}], UserID: [{}]", doctorId, userId);

        validateDoctorId(doctorId);

        Date[] appointmentDateRange = getDateRange(appointmentSearchDTO.getAppointmentDate());
        Date[] bookingDateRange = getDateRange(appointmentSearchDTO.getBookingDate());

        AppointmentStatus status = null;
        Boolean treated = null;

        if (StringUtils.hasText(appointmentSearchDTO.getStatus())) {
            String statusStr = appointmentSearchDTO.getStatus().toUpperCase();
            if ("TREATED".equals(statusStr)) {
                treated = true;
            } else if ("PENDING".equals(statusStr)) {
                status = AppointmentStatus.BOOKED;
            } else if (!"ALL".equals(statusStr)) {
                try {
                    status =AppointmentStatus.valueOf(statusStr);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid status provided for search: {}", statusStr);
                    // treat as null (ALL) if invalid, or could throw exception
                }
            }
        }

        List<AppointmentEntity> appointments = appointmentRepository.searchAppointmentsByDoctorId(
                doctorId,
                appointmentSearchDTO.getAppointmentId(),
                appointmentSearchDTO.getPatientName(),
                appointmentSearchDTO.getContact(),
                appointmentSearchDTO.getEmail(),
                appointmentDateRange[0],
                appointmentDateRange[1],
                bookingDateRange[0],
                bookingDateRange[1],
                status,
                appointmentSearchDTO.getAppointmentType(),
                treated
        );

        logger.debug("Found {} appointments matching criteria.", appointments.size());

        return appointments.stream()
                .map(appointment -> modelMapper.map(appointment, AppointmentDTO.class))
                .toList();
    }

    private void validateDoctorId(String doctorId) {
        if (!StringUtils.hasText(doctorId)) {
            logger.error("Appointment search failed: Missing Doctor ID.");
            throw new ResourceNotFoundException("Doctor ID cannot be null or empty.");
        }
    }

    private Date[] getDateRange(String date) {
        if (!StringUtils.hasText(date)) {
            return new Date[]{null, null};
        }
        return DateUtils.getStartAndEndOfDay(date);
    }
}
