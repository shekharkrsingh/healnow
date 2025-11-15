package com.heal.doctor.services.impl;

import com.heal.doctor.dto.AppointmentDTO;
import com.heal.doctor.dto.AppointmentRequestDTO;
import com.heal.doctor.dto.WebSocketResponseType;
import com.heal.doctor.dto.WebsocketResponseDTO;
import com.heal.doctor.models.AppointmentEntity;
import com.heal.doctor.models.NotificationEntity;
import com.heal.doctor.models.enums.AppointmentStatus;
import com.heal.doctor.models.enums.AppointmentType;
import com.heal.doctor.models.enums.NotificationType;
import com.heal.doctor.repositories.AppointmentRepository;
import com.heal.doctor.services.IAppointmentService;
import com.heal.doctor.services.INotificationService;
import com.heal.doctor.exception.BusinessRuleException;
import com.heal.doctor.exception.ConflictException;
import com.heal.doctor.exception.ForbiddenException;
import com.heal.doctor.exception.ResourceNotFoundException;
import com.heal.doctor.exception.ValidationException;
import com.heal.doctor.utils.AppointmentId;
import com.heal.doctor.utils.CurrentUserName;
import com.heal.doctor.utils.DateUtils;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@AllArgsConstructor
@Service
public class AppointmentServiceImpl implements IAppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentServiceImpl.class);
    private static final int VALID_CONTACT_LENGTH = 10;

    private final AppointmentRepository appointmentRepository;
    private final ModelMapper modelMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final INotificationService notificationService;


    @Transactional
    @Override
    public AppointmentDTO bookAppointment(AppointmentRequestDTO requestDTO) {
        String doctorId = CurrentUserName.getCurrentDoctorId();
        logger.info("Booking appointment for doctorId: {}, patientName: {}", doctorId, requestDTO.getPatientName());

        if (requestDTO.getPatientName() == null || requestDTO.getPatientName().trim().isEmpty()) {
            logger.warn("Appointment booking failed: Patient name is empty for doctorId: {}", doctorId);
            throw new ValidationException("Patient name is required and cannot be empty.");
        }

        if (requestDTO.getPaymentStatus() == null) {
            logger.warn("Appointment booking failed: Payment status is null for doctorId: {}", doctorId);
            throw new ValidationException("Payment status is required.");
        }

        if (requestDTO.getAvailableAtClinic() == null) {
            logger.warn("Appointment booking failed: Available at clinic status is null for doctorId: {}", doctorId);
            throw new ValidationException("Availability at clinic is required.");
        }

        if (requestDTO.getContact() == null || requestDTO.getContact().trim().isEmpty()) {
            logger.warn("Appointment booking failed: Contact is empty for doctorId: {}, patientName: {}", doctorId, requestDTO.getPatientName());
            throw new ValidationException("Contact number is required and cannot be empty.");
        }

        if (requestDTO.getContact().trim().length() != VALID_CONTACT_LENGTH) {
            logger.warn("Appointment booking failed: Invalid contact length for doctorId: {}, contact: {}", doctorId, requestDTO.getContact());
            throw new ValidationException("Contact number must be exactly " + VALID_CONTACT_LENGTH + " digits.");
        }

        Date appointmentDate = requestDTO.getAppointmentDateTime() != null ? requestDTO.getAppointmentDateTime() : new Date();
        Date[] date = DateUtils.getStartAndEndOfDay(new Date());

        boolean exists = appointmentRepository.existsByDoctorIdAndPatientNameAndContactAndAppointmentDateTimeBetweenAndStatus(
                doctorId,
                requestDTO.getPatientName(),
                requestDTO.getContact(),
                date[0],
                date[1],
                AppointmentStatus.ACCEPTED);

        if (exists) {
            logger.warn("Appointment booking failed: Duplicate appointment exists for doctorId: {}, patientName: {}, contact: {}", 
                    doctorId, requestDTO.getPatientName(), requestDTO.getContact());
            throw new ConflictException("Appointment", "An appointment for this patient already exists on the selected date.");
        }

        AppointmentEntity appointmentEntity = modelMapper.map(requestDTO, AppointmentEntity.class);
        appointmentEntity.setStatus(AppointmentStatus.ACCEPTED);
        appointmentEntity.setAppointmentDateTime(appointmentDate);
        appointmentEntity.setBookingDateTime(new Date());
        appointmentEntity.setDoctorId(doctorId);
        appointmentEntity.setAppointmentId(AppointmentId.generateAppointmentId(doctorId));
        appointmentEntity.setTreated(false);
        appointmentEntity.setAppointmentType(AppointmentType.IN_PERSON);
        appointmentEntity.setIsEmergency(false);
        AppointmentEntity savedAppointment = appointmentRepository.save(appointmentEntity);

        logger.info("Appointment booked successfully: appointmentId: {}, doctorId: {}, patientName: {}", 
                savedAppointment.getAppointmentId(), doctorId, requestDTO.getPatientName());

        AppointmentDTO appointmentDTO = modelMapper.map(savedAppointment, AppointmentDTO.class);

        if (removeTime(appointmentDate).equals(removeTime(new Date()))) {
            logger.debug("Sending WebSocket notification for today's appointment: appointmentId: {}, doctorId: {}", 
                    appointmentDTO.getAppointmentId(), appointmentDTO.getDoctorId());
            messagingTemplate.convertAndSend("/topic/appointments/" + appointmentDTO.getDoctorId(),
                    WebsocketResponseDTO.<AppointmentDTO>builderGeneric()
                            .type(WebSocketResponseType.APPOINTMENT)
                            .payload(appointmentDTO)
                            .build());
        }
        return appointmentDTO;
    }

    @Transactional
    @Override
    public AppointmentDTO updateEmergencyStatus(String appointmentId, Boolean isEmergency) {
        logger.info("Updating emergency status: appointmentId: {}, isEmergency: {}", appointmentId, isEmergency);
        AppointmentEntity appointmentEntity = appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        String currentDoctorId = appointmentEntity.getDoctorId();
        String requestingDoctorId = CurrentUserName.getCurrentDoctorId();
        if (!currentDoctorId.equals(requestingDoctorId)) {
            logger.warn("Unauthorized emergency status update attempt: appointmentId: {}, owner: {}, requester: {}", 
                    appointmentId, currentDoctorId, requestingDoctorId);
            throw new ForbiddenException("appointment", "update");
        }
        AppointmentEntity newAppointmentEntity = appointmentEntity;
        if (!appointmentEntity.getIsEmergency().equals(isEmergency)) {
            appointmentEntity.setIsEmergency(isEmergency);
            newAppointmentEntity = appointmentRepository.save(appointmentEntity);
            logger.info("Emergency status updated: appointmentId: {}, isEmergency: {}, doctorId: {}", 
                    appointmentId, isEmergency, currentDoctorId);
            if (newAppointmentEntity.getIsEmergency()) {
                logger.info("Emergency appointment notification created: appointmentId: {}, doctorId: {}", 
                        appointmentId, currentDoctorId);
                NotificationEntity notification = NotificationEntity.builder().
                        doctorId(appointmentEntity.getDoctorId()).
                        type(NotificationType.EMERGENCY).
                        title("New Emergency Appointment Alert").
                        message("A new emergency appointment has been registered. Please check and take immediate action.").
                        build();
                notificationService.createNotificationAsync(notification).exceptionally(ex -> {
                    logger.error("Failed to create emergency notification asynchronously: appointmentId: {}, doctorId: {}, error: {}", 
                            appointmentId, appointmentEntity.getDoctorId(), ex.getMessage(), ex);
                    return null;
                });
            }
        }
        AppointmentDTO appointmentDTO = modelMapper.map(newAppointmentEntity, AppointmentDTO.class);

        if (removeTime(appointmentDTO.getAppointmentDateTime()).equals(removeTime(new Date()))) {
            messagingTemplate.convertAndSend("/topic/appointments/" + appointmentDTO.getDoctorId(), WebsocketResponseDTO.<AppointmentDTO>builderGeneric()
                    .type(WebSocketResponseType.APPOINTMENT)
                    .payload(appointmentDTO)
                    .build());
        }
        return appointmentDTO;
    }


    @Override
    public AppointmentDTO getAppointmentById(String appointmentId) {
        logger.debug("Fetching appointment: appointmentId: {}", appointmentId);
        AppointmentEntity appointmentEntity = appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        String currentDoctorId = appointmentEntity.getDoctorId();
        String requestingDoctorId = CurrentUserName.getCurrentDoctorId();
        if (!currentDoctorId.equals(requestingDoctorId)) {
            logger.warn("Unauthorized appointment access attempt: appointmentId: {}, owner: {}, requester: {}", 
                    appointmentId, currentDoctorId, requestingDoctorId);
            throw new ForbiddenException("appointment", "view");
        }
        logger.debug("Appointment retrieved: appointmentId: {}, doctorId: {}", appointmentId, currentDoctorId);
        return modelMapper.map(appointmentEntity, AppointmentDTO.class);
    }

    @Override
    public List<AppointmentDTO> getAppointmentsByBookingDate(String date) {
        String currentDoctor = CurrentUserName.getCurrentDoctorId();
        logger.debug("Fetching appointments by booking date: doctorId: {}, date: {}", currentDoctor, date);

        Date[] startAndEnd = DateUtils.getStartAndEndOfDay(date);
        List<AppointmentEntity> appointments = appointmentRepository.
                findByDoctorIdAndBookingDateTimeBetween(currentDoctor, startAndEnd[0], startAndEnd[1]);

        logger.debug("Found {} appointments for doctorId: {}, date: {}", appointments.size(), currentDoctor, date);
        return appointments
                .stream()
                .map(appointment -> modelMapper.map(appointment, AppointmentDTO.class))
                .toList();
    }

    @Transactional
    @Override
    public AppointmentDTO updateAppointmentStatus(String appointmentId, AppointmentStatus status) {
        logger.info("Updating appointment status: appointmentId: {}, newStatus: {}", appointmentId, status);
        AppointmentEntity appointmentEntity = appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        String currentDoctorId = appointmentEntity.getDoctorId();
        String requestingDoctorId = CurrentUserName.getCurrentDoctorId();
        if (!currentDoctorId.equals(requestingDoctorId)) {
            logger.warn("Unauthorized status update attempt: appointmentId: {}, owner: {}, requester: {}, status: {}", 
                    appointmentId, currentDoctorId, requestingDoctorId, status);
            throw new ForbiddenException("appointment", "update");
        }
        AppointmentStatus oldStatus = appointmentEntity.getStatus();
        appointmentEntity.setStatus(status);
        AppointmentEntity updatedAppointment = appointmentRepository.save(appointmentEntity);
        logger.info("Appointment status updated: appointmentId: {}, oldStatus: {}, newStatus: {}, doctorId: {}", 
                appointmentId, oldStatus, status, currentDoctorId);
        AppointmentDTO appointmentDTO = modelMapper.map(updatedAppointment, AppointmentDTO.class);

        if (removeTime(appointmentDTO.getAppointmentDateTime()).equals(removeTime(new Date()))) {
            logger.debug("Sending WebSocket notification for status update: appointmentId: {}, status: {}", 
                    appointmentId, status);
            messagingTemplate.convertAndSend("/topic/appointments/" + appointmentDTO.getDoctorId(), WebsocketResponseDTO.<AppointmentDTO>builderGeneric()
                    .type(WebSocketResponseType.APPOINTMENT)
                    .payload(appointmentDTO)
                    .build());
        }

        return appointmentDTO;
    }

    @Transactional
    @Override
    public AppointmentDTO updatePaymentStatus(String appointmentId, Boolean paymentStatus) {
        logger.info("Updating payment status: appointmentId: {}, paymentStatus: {}", appointmentId, paymentStatus);
        AppointmentEntity appointmentEntity = appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        String currentDoctorId = appointmentEntity.getDoctorId();
        String requestingDoctorId = CurrentUserName.getCurrentDoctorId();
        if (!currentDoctorId.equals(requestingDoctorId)) {
            logger.warn("Unauthorized payment status update attempt: appointmentId: {}, owner: {}, requester: {}", 
                    appointmentId, currentDoctorId, requestingDoctorId);
            throw new ForbiddenException("appointment", "update");
        }

        if (
                (appointmentEntity.getStatus().equals(AppointmentStatus.CANCELLED) || appointmentEntity.getStatus().equals(AppointmentStatus.BOOKED))
                        && !appointmentEntity.getPaymentStatus()
                        && paymentStatus
        ) {
            logger.warn("Payment status update failed - invalid status: appointmentId: {}, currentStatus: {}, paymentStatus: {}", 
                    appointmentId, appointmentEntity.getStatus(), paymentStatus);
            throw new BusinessRuleException("mark as paid", "Appointment must be in ACCEPTED status");
        }
        Boolean oldPaymentStatus = appointmentEntity.getPaymentStatus();
        appointmentEntity.setPaymentStatus(paymentStatus);
        AppointmentEntity updatedAppointment = appointmentRepository.save(appointmentEntity);
        logger.info("Payment status updated: appointmentId: {}, oldStatus: {}, newStatus: {}, doctorId: {}", 
                appointmentId, oldPaymentStatus, paymentStatus, currentDoctorId);

        AppointmentDTO appointmentDTO = modelMapper.map(updatedAppointment, AppointmentDTO.class);

        if (removeTime(appointmentDTO.getAppointmentDateTime()).equals(removeTime(new Date()))) {
            logger.debug("Sending WebSocket notification for payment status update: appointmentId: {}", appointmentId);
            messagingTemplate.convertAndSend("/topic/appointments/" + appointmentDTO.getDoctorId(),
                    WebsocketResponseDTO.<AppointmentDTO>builderGeneric()
                            .type(WebSocketResponseType.APPOINTMENT)
                            .payload(appointmentDTO)
                            .build());
        }
        return appointmentDTO;
    }


    @Transactional
    @Override
    public AppointmentDTO updateTreatedStatus(String appointmentId, Boolean treatedStatus) {
        logger.info("Updating treated status: appointmentId: {}, treatedStatus: {}", appointmentId, treatedStatus);
        AppointmentEntity appointmentEntity = appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));

        String currentDoctorId = appointmentEntity.getDoctorId();
        String requestingDoctorId = CurrentUserName.getCurrentDoctorId();
        if (!currentDoctorId.equals(requestingDoctorId)) {
            logger.warn("Unauthorized treated status update attempt: appointmentId: {}, owner: {}, requester: {}", 
                    appointmentId, currentDoctorId, requestingDoctorId);
            throw new ForbiddenException("appointment", "update");
        }

        if (!appointmentEntity.getPaymentStatus()) {
            logger.warn("Treated status update failed - payment pending: appointmentId: {}, doctorId: {}", 
                    appointmentId, currentDoctorId);
            throw new BusinessRuleException("mark as treated", "Payment is pending");
        }

        if (!appointmentEntity.getAvailableAtClinic()) {
            logger.warn("Treated status update failed - patient not at clinic: appointmentId: {}, doctorId: {}", 
                    appointmentId, currentDoctorId);
            throw new BusinessRuleException("mark as treated", "Patient is not available at the clinic");
        }

        if (appointmentEntity.getStatus().equals(AppointmentStatus.CANCELLED) || appointmentEntity.getStatus().equals(AppointmentStatus.BOOKED)) {
            logger.warn("Treated status update failed - invalid appointment status: appointmentId: {}, status: {}, doctorId: {}", 
                    appointmentId, appointmentEntity.getStatus(), currentDoctorId);
            throw new BusinessRuleException("mark as treated", "Appointment must be in ACCEPTED status");
        }

        Boolean oldTreatedStatus = appointmentEntity.getTreated();
        appointmentEntity.setTreatedDateTime(new Date());
        appointmentEntity.setTreated(treatedStatus);

        AppointmentEntity updatedAppointment = appointmentRepository.save(appointmentEntity);
        logger.info("Treated status updated: appointmentId: {}, oldStatus: {}, newStatus: {}, doctorId: {}", 
                appointmentId, oldTreatedStatus, treatedStatus, currentDoctorId);

        AppointmentDTO appointmentDTO = modelMapper.map(updatedAppointment, AppointmentDTO.class);
        if (removeTime(appointmentDTO.getAppointmentDateTime()).equals(removeTime(new Date()))) {
            logger.debug("Sending WebSocket notification for treated status update: appointmentId: {}", appointmentId);
            messagingTemplate.convertAndSend("/topic/appointments/" + appointmentDTO.getDoctorId(),
                    WebsocketResponseDTO.<AppointmentDTO>builderGeneric()
                            .type(WebSocketResponseType.APPOINTMENT)
                            .payload(appointmentDTO)
                            .build());
        }

        return appointmentDTO;
    }

    @Transactional
    @Override
    public AppointmentDTO updateAvailableAtClinic(String appointmentId, Boolean availableAtClinicStatus) {
        logger.info("Updating available at clinic status: appointmentId: {}, availableAtClinic: {}", appointmentId, availableAtClinicStatus);
        AppointmentEntity appointmentEntity = appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));

        String currentDoctorId = appointmentEntity.getDoctorId();
        String requestingDoctorId = CurrentUserName.getCurrentDoctorId();
        if (!currentDoctorId.equals(requestingDoctorId)) {
            logger.warn("Unauthorized availability update attempt: appointmentId: {}, owner: {}, requester: {}", 
                    appointmentId, currentDoctorId, requestingDoctorId);
            throw new ForbiddenException("appointment", "update");
        }

        if (appointmentEntity.getTreated()) {
            logger.warn("Availability update failed - already treated: appointmentId: {}, doctorId: {}", 
                    appointmentId, currentDoctorId);
            throw new BusinessRuleException("update availability", "Patient is already treated");
        }

        if (appointmentEntity.getStatus().equals(AppointmentStatus.CANCELLED) || appointmentEntity.getStatus().equals(AppointmentStatus.BOOKED)) {
            logger.warn("Availability update failed - invalid status: appointmentId: {}, status: {}, doctorId: {}", 
                    appointmentId, appointmentEntity.getStatus(), currentDoctorId);
            throw new BusinessRuleException("mark as available", "Appointment must be in ACCEPTED status");
        }
        Boolean oldAvailableAtClinic = appointmentEntity.getAvailableAtClinic();
        appointmentEntity.setAvailableAtClinic(availableAtClinicStatus);

        AppointmentEntity updatedAppointment = appointmentRepository.save(appointmentEntity);
        logger.info("Availability at clinic updated: appointmentId: {}, oldStatus: {}, newStatus: {}, doctorId: {}", 
                appointmentId, oldAvailableAtClinic, availableAtClinicStatus, currentDoctorId);

        AppointmentDTO appointmentDTO = modelMapper.map(updatedAppointment, AppointmentDTO.class);

        if (removeTime(appointmentDTO.getAppointmentDateTime()).equals(removeTime(new Date()))) {
            logger.debug("Sending WebSocket notification for availability update: appointmentId: {}", appointmentId);
            messagingTemplate.convertAndSend("/topic/appointments/" + appointmentDTO.getDoctorId(),
                    WebsocketResponseDTO.<AppointmentDTO>builderGeneric()
                            .type(WebSocketResponseType.APPOINTMENT)
                            .payload(appointmentDTO)
                            .build());
        }

        return appointmentDTO;
    }

    @Transactional
    @Override
    public AppointmentDTO cancelAppointment(String appointmentId){
        logger.info("Cancelling appointment: appointmentId: {}", appointmentId);
        AppointmentEntity appointmentEntity = appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        String currentDoctorId = appointmentEntity.getDoctorId();
        String requestingDoctorId = CurrentUserName.getCurrentDoctorId();
        if (!currentDoctorId.equals(requestingDoctorId)) {
            logger.warn("Unauthorized cancellation attempt: appointmentId: {}, owner: {}, requester: {}", 
                    appointmentId, currentDoctorId, requestingDoctorId);
            throw new ForbiddenException("appointment", "cancel");
        }
        if (appointmentEntity.getTreated()) {
            logger.warn("Cancellation failed - already treated: appointmentId: {}, doctorId: {}", 
                    appointmentId, currentDoctorId);
            throw new BusinessRuleException("cancel appointment", "Patient is already treated");
        }
        if (Boolean.TRUE.equals(appointmentEntity.getPaymentStatus())) {
            logger.warn("Cancellation failed - payment received: appointmentId: {}, doctorId: {}", 
                    appointmentId, currentDoctorId);
            throw new BusinessRuleException("cancel appointment", "Payment has already been received");
        }
        AppointmentStatus oldStatus = appointmentEntity.getStatus();
        appointmentEntity.setStatus(AppointmentStatus.CANCELLED);

        AppointmentEntity updatedAppointment = appointmentRepository.save(appointmentEntity);
        logger.info("Appointment cancelled: appointmentId: {}, oldStatus: {}, doctorId: {}, patientName: {}", 
                appointmentId, oldStatus, currentDoctorId, appointmentEntity.getPatientName());

        AppointmentDTO appointmentDTO = modelMapper.map(updatedAppointment, AppointmentDTO.class);

        if (removeTime(appointmentDTO.getAppointmentDateTime()).equals(removeTime(new Date()))) {
            logger.debug("Sending WebSocket notification for cancellation: appointmentId: {}", appointmentId);
            messagingTemplate.convertAndSend("/topic/appointments/" + appointmentDTO.getDoctorId(),
                    WebsocketResponseDTO.<AppointmentDTO>builderGeneric()
                            .type(WebSocketResponseType.APPOINTMENT)
                            .payload(appointmentDTO)
                            .build());
        }

        return appointmentDTO;
    }

    @Override
    public List<AppointmentDTO> getAppointmentsByDoctorAndDateRange(String doctorId, String fromDate, String toDate) {
        logger.debug("Fetching appointments by date range: doctorId: {}, fromDate: {}, toDate: {}", doctorId, fromDate, toDate);
        Date[] fromDateRange = DateUtils.getStartAndEndOfDay(fromDate);
        Date[] toDateRange = DateUtils.getStartAndEndOfDay(toDate);

        Date startDate = fromDateRange[0];
        Date endDate = toDateRange[1];

        List<AppointmentEntity> appointments = appointmentRepository
                .findByDoctorIdAndAppointmentDateTimeBetween(
                        doctorId,
                        startDate,
                        endDate
                );

        logger.debug("Found {} appointments for doctorId: {}, dateRange: {} to {}", 
                appointments.size(), doctorId, fromDate, toDate);
        return appointments.stream()
                .map(appointment -> modelMapper.map(appointment, AppointmentDTO.class))
                .toList();
    }



    private Date removeTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
