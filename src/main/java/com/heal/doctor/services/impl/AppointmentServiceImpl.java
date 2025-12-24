package com.heal.doctor.services.impl;

import com.heal.doctor.dto.AppointmentDTO;
import com.heal.doctor.dto.AppointmentRequestDTO;
import com.heal.doctor.dto.WebSocketResponseType;
import com.heal.doctor.dto.WebsocketResponseDTO;
import com.heal.doctor.models.AppointmentEntity;
import com.heal.doctor.models.NotificationEntity;
import com.heal.doctor.models.enums.AppointmentStatus;
import com.heal.doctor.models.enums.AppointmentType;
import com.heal.doctor.models.enums.NotificationRecipientType;
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
import com.heal.doctor.utils.RoleUtils;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
        Date currentTime = new Date();
        
        if (!Boolean.TRUE.equals(requestDTO.getAvailableAtClinic())) {
            if (appointmentDate.before(currentTime) || appointmentDate.equals(currentTime)) {
                logger.warn("Scheduled appointment cannot be in the past or present: doctorId: {}, appointmentDate: {}, currentTime: {}", 
                    doctorId, appointmentDate, currentTime);
                throw new ValidationException("Scheduled appointments must be in the future.");
            }
        } else {
            long oneHourInMillis = 60 * 60 * 1000;
            Date oneHourAgo = new Date(currentTime.getTime() - oneHourInMillis);
            if (appointmentDate.before(oneHourAgo)) {
                logger.info("Walk-in appointment date adjusted from {} to current time: doctorId: {}", 
                    appointmentDate, doctorId);
                appointmentDate = new Date();
            }
        }
        
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
        if (Boolean.TRUE.equals(requestDTO.getAvailableAtClinic())) {
            appointmentEntity.setAvailableAtClinicDateTime(new Date());
        }
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
        String requestingUserId = CurrentUserName.getCurrentUserId();
        if (!RoleUtils.isAdminOrOwnerOrCollaborator(currentDoctorId, requestingUserId)) {
            logger.warn("Unauthorized emergency status update attempt: appointmentId: {}, owner: {}, requester: {}", 
                    appointmentId, currentDoctorId, requestingUserId);
            throw new ForbiddenException("appointment", "update");
        }
        AppointmentEntity newAppointmentEntity = appointmentEntity;
        if (!Objects.equals(appointmentEntity.getIsEmergency(), isEmergency)) {
            appointmentEntity.setIsEmergency(isEmergency);
            newAppointmentEntity = appointmentRepository.save(appointmentEntity);
            logger.info("Emergency status updated: appointmentId: {}, isEmergency: {}, doctorId: {}", 
                    appointmentId, isEmergency, currentDoctorId);
            if (newAppointmentEntity.getIsEmergency()) {
                logger.info("Emergency appointment notification created: appointmentId: {}, doctorId: {}", 
                        appointmentId, currentDoctorId);
                NotificationEntity notification = NotificationEntity.builder().
                        targetId(CurrentUserName.getCurrentDoctorId()).
                        recipientType(NotificationRecipientType.DOCTOR_COLLABORATORS).
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
        String requestingUserId = CurrentUserName.getCurrentUserId();
        if (!RoleUtils.isAdminOrOwnerOrCollaborator(currentDoctorId, requestingUserId)) {
            logger.warn("Unauthorized appointment access attempt: appointmentId: {}, owner: {}, requester: {}", 
                    appointmentId, currentDoctorId, requestingUserId);
            throw new ForbiddenException("appointment", "view");
        }
        logger.debug("Appointment retrieved: appointmentId: {}, doctorId: {}", appointmentId, currentDoctorId);
        return modelMapper.map(appointmentEntity, AppointmentDTO.class);
    }

    @Override
    public List<AppointmentDTO> getAppointmentsByBookingDate(String date) {
        String currentDoctor = CurrentUserName.getCurrentDoctorId();
        logger.debug("Fetching appointments by appointment date: doctorId: {}, date: {}", currentDoctor, date);

        Date[] startAndEnd = DateUtils.getStartAndEndOfDay(date);
        Date currentTime = new Date();
        
        List<AppointmentEntity> appointments = appointmentRepository.
                findByDoctorIdAndAppointmentDateTimeBetween(currentDoctor, startAndEnd[0], startAndEnd[1]);

        logger.debug("Found {} appointments for doctorId: {}, date: {}", appointments.size(), currentDoctor, date);
        
        List<AppointmentDTO> allAppointments = appointments
                .parallelStream()
                .map(appointment -> modelMapper.map(appointment, AppointmentDTO.class))
                .toList();
        
        List<AppointmentDTO> activeAppointments = allAppointments.stream()
                .filter(appointment -> appointment.getStatus() != AppointmentStatus.CANCELLED)
                .sorted(createFairQueueComparator(currentTime))
                .collect(Collectors.toList());
        
        List<AppointmentDTO> cancelledAppointments = allAppointments.stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.CANCELLED)
                .sorted((a, b) -> b.getBookingDateTime().compareTo(a.getBookingDateTime()))
                .toList();
        
        activeAppointments.addAll(cancelledAppointments);
        return activeAppointments;
    }

    @Transactional
    @Override
    public AppointmentDTO updateAppointmentStatus(String appointmentId, AppointmentStatus status) {
        logger.info("Updating appointment status: appointmentId: {}, newStatus: {}", appointmentId, status);
        AppointmentEntity appointmentEntity = appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        String currentDoctorId = appointmentEntity.getDoctorId();
        String requestingUserId = CurrentUserName.getCurrentUserId();
        if (!RoleUtils.isAdminOrOwnerOrCollaborator(currentDoctorId, requestingUserId)) {
            logger.warn("Unauthorized status update attempt: appointmentId: {}, owner: {}, requester: {}, status: {}", 
                    appointmentId, currentDoctorId, requestingUserId, status);
            throw new ForbiddenException("appointment", "update");
        }
        AppointmentStatus oldStatus = appointmentEntity.getStatus();
        
        if (oldStatus.equals(AppointmentStatus.CANCELLED) && status.equals(AppointmentStatus.ACCEPTED)) {
            if (Boolean.TRUE.equals(appointmentEntity.getPaymentStatus())) {
                logger.warn("Cannot restore cancelled appointment with payment: appointmentId: {}, doctorId: {}", 
                        appointmentId, currentDoctorId);
                throw new BusinessRuleException("restore appointment", "Cannot restore cancelled appointment with payment already received");
            }
            if (appointmentEntity.getTreated()) {
                logger.warn("Cannot restore cancelled appointment that was treated: appointmentId: {}, doctorId: {}", 
                        appointmentId, currentDoctorId);
                throw new BusinessRuleException("restore appointment", "Cannot restore cancelled appointment that was already treated");
            }
        }
        
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
        String requestingUserId = CurrentUserName.getCurrentUserId();
        if (!RoleUtils.isAdminOrOwnerOrCollaborator(currentDoctorId, requestingUserId)) {
            logger.warn("Unauthorized payment status update attempt: appointmentId: {}, owner: {}, requester: {}", 
                    appointmentId, currentDoctorId, requestingUserId);
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
        String requestingUserId = CurrentUserName.getCurrentUserId();
        if (!RoleUtils.isAdminOrOwnerOrCollaborator(currentDoctorId, requestingUserId)) {
            logger.warn("Unauthorized treated status update attempt: appointmentId: {}, owner: {}, requester: {}", 
                    appointmentId, currentDoctorId, requestingUserId);
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
        String requestingUserId = CurrentUserName.getCurrentUserId();
        if (!RoleUtils.isAdminOrOwnerOrCollaborator(currentDoctorId, requestingUserId)) {
            logger.warn("Unauthorized availability update attempt: appointmentId: {}, owner: {}, requester: {}", 
                    appointmentId, currentDoctorId, requestingUserId);
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
        
        if (availableAtClinicStatus && !oldAvailableAtClinic) {
            appointmentEntity.setAvailableAtClinicDateTime(new Date());
        } else if (!availableAtClinicStatus && oldAvailableAtClinic) {
            appointmentEntity.setAvailableAtClinicDateTime(null);
        }
        
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
        String requestingUserId = CurrentUserName.getCurrentUserId();
        if (!RoleUtils.isAdminOrOwnerOrCollaborator(currentDoctorId, requestingUserId)) {
            logger.warn("Unauthorized cancellation attempt: appointmentId: {}, owner: {}, requester: {}", 
                    appointmentId, currentDoctorId, requestingUserId);
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
        return appointments.parallelStream()
                .map(appointment -> modelMapper.map(appointment, AppointmentDTO.class))
                .collect(Collectors.toList());
    }



    private Comparator<AppointmentDTO> createFairQueueComparator(Date currentTime) {
        return Comparator
                .comparing(AppointmentDTO::getTreated)
                .thenComparing(AppointmentDTO::getIsEmergency, Comparator.reverseOrder())
                .thenComparing((a, b) -> compareStatus(a.getStatus(), b.getStatus()))
                .thenComparing((a, b) -> {
                    boolean aOverdue = currentTime.after(a.getAppointmentDateTime());
                    boolean bOverdue = currentTime.after(b.getAppointmentDateTime());
                    
                    long aTimeDiff = a.getAppointmentDateTime().getTime() - currentTime.getTime();
                    long bTimeDiff = b.getAppointmentDateTime().getTime() - currentTime.getTime();
                    boolean aCurrent = !aOverdue && aTimeDiff <= 5 * 60 * 1000;
                    boolean bCurrent = !bOverdue && bTimeDiff <= 5 * 60 * 1000;
                    
                    int aGroup = aOverdue ? 1 : (aCurrent ? 2 : 3);
                    int bGroup = bOverdue ? 1 : (bCurrent ? 2 : 3);
                    
                    if (aGroup != bGroup) {
                        return Integer.compare(aGroup, bGroup);
                    }
                    
                    if (aGroup == 1 || aGroup == 2) {
                        int apptCompare = a.getAppointmentDateTime().compareTo(b.getAppointmentDateTime());
                        if (apptCompare != 0) return apptCompare;
                        
                        int availCompare = Boolean.compare(b.getAvailableAtClinic(), a.getAvailableAtClinic());
                        if (availCompare != 0) return availCompare;
                        
                        if (a.getAvailableAtClinic() && b.getAvailableAtClinic()) {
                            Date aArrival = a.getAvailableAtClinicDateTime();
                            Date bArrival = b.getAvailableAtClinicDateTime();
                            if (aArrival != null && bArrival != null) {
                                return aArrival.compareTo(bArrival);
                            } else if (aArrival != null) return -1;
                            else if (bArrival != null) return 1;
                        }
                    } else {
                        int availCompare = Boolean.compare(b.getAvailableAtClinic(), a.getAvailableAtClinic());
                        if (availCompare != 0) return availCompare;
                        
                        if (a.getAvailableAtClinic() && b.getAvailableAtClinic()) {
                            Date aArrival = a.getAvailableAtClinicDateTime();
                            Date bArrival = b.getAvailableAtClinicDateTime();
                            if (aArrival != null && bArrival != null) {
                                long aWaiting = currentTime.getTime() - aArrival.getTime();
                                long bWaiting = currentTime.getTime() - bArrival.getTime();
                                return Long.compare(bWaiting, aWaiting);
                            } else if (aArrival != null) return -1;
                            else if (bArrival != null) return 1;
                        }
                        
                        return a.getAppointmentDateTime().compareTo(b.getAppointmentDateTime());
                    }
                    
                    return 0;
                })
                .thenComparing(AppointmentDTO::getBookingDateTime);
    }

    private int compareStatus(AppointmentStatus a, AppointmentStatus b) {
        Map<AppointmentStatus, Integer> priority = Map.of(
                AppointmentStatus.ACCEPTED, 1,
                AppointmentStatus.BOOKED, 2,
                AppointmentStatus.CANCELLED, 3
        );
        return Integer.compare(
                priority.getOrDefault(a, 99),
                priority.getOrDefault(b, 99)
        );
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
