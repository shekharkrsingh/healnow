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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@AllArgsConstructor
@Service
public class AppointmentServiceImpl implements IAppointmentService {


    private final AppointmentRepository appointmentRepository;
    private final ModelMapper modelMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final INotificationService notificationService;


    @Transactional
    @Override
    public AppointmentDTO bookAppointment(AppointmentRequestDTO requestDTO) {
        if (requestDTO.getPatientName() == null || requestDTO.getPatientName().trim().isEmpty()) {
            throw new ValidationException("Patient name is required and cannot be empty.");
        }

        if (requestDTO.getPaymentStatus() == null) {
            throw new ValidationException("Payment status is required.");
        }

        if (requestDTO.getAvailableAtClinic() == null) {
            throw new ValidationException("Availability at clinic is required.");
        }

        if (requestDTO.getContact() == null || requestDTO.getContact().trim().isEmpty()) {
            throw new ValidationException("Contact number is required and cannot be empty.");
        }

        if (requestDTO.getContact().trim().length() != 10) {
            throw new ValidationException("Contact number must be exactly 10 digits.");
        }

        String doctorId = CurrentUserName.getCurrentDoctorId();
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

        AppointmentDTO appointmentDTO = modelMapper.map(savedAppointment, AppointmentDTO.class);

        if (removeTime(appointmentDate).equals(removeTime(new Date()))) {
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
        AppointmentEntity appointmentEntity = appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        String currentDoctorId = appointmentEntity.getDoctorId();
        if (!currentDoctorId.equals(CurrentUserName.getCurrentDoctorId())) {
            throw new ForbiddenException("appointment", "update");
        }
        AppointmentEntity newAppointmentEntity = appointmentEntity;
        if (!appointmentEntity.getIsEmergency().equals(isEmergency)) {
            appointmentEntity.setIsEmergency(isEmergency);
            newAppointmentEntity = appointmentRepository.save(appointmentEntity);
            if (newAppointmentEntity.getIsEmergency()) {
                NotificationEntity notification = NotificationEntity.builder().
                        doctorId(appointmentEntity.getDoctorId()).
                        type(NotificationType.EMERGENCY).
                        title("New Emergency Appointment Alert").
                        message("A new emergency appointment has been registered. Please check and take immediate action.").
                        build();
                notificationService.createNotification(notification);
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
        AppointmentEntity appointmentEntity = appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        String currentDoctorId = appointmentEntity.getDoctorId();
        if (!currentDoctorId.equals(CurrentUserName.getCurrentDoctorId())) {
            throw new ForbiddenException("appointment", "view");
        }
        return modelMapper.map(appointmentEntity, AppointmentDTO.class);
    }

    @Override
    public List<AppointmentDTO> getAppointmentsByBookingDate(String date) {
        String currentDoctor = CurrentUserName.getCurrentDoctorId();

        Date[] startAndEnd = DateUtils.getStartAndEndOfDay(date);
        List<AppointmentEntity> appointments = appointmentRepository.
                findByDoctorIdAndBookingDateTimeBetween(currentDoctor, startAndEnd[0], startAndEnd[1]);

        return appointments
                .stream()
                .map(appointment -> modelMapper.map(appointment, AppointmentDTO.class))
                .toList();
    }

    @Transactional
    @Override
    public AppointmentDTO updateAppointmentStatus(String appointmentId, AppointmentStatus status) {
        AppointmentEntity appointmentEntity = appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        String currentDoctorId = appointmentEntity.getDoctorId();
        if (!currentDoctorId.equals(CurrentUserName.getCurrentDoctorId())) {
            throw new ForbiddenException("appointment", "update");
        }
        appointmentEntity.setStatus(status);
        AppointmentEntity updatedAppointment = appointmentRepository.save(appointmentEntity);
        AppointmentDTO appointmentDTO = modelMapper.map(updatedAppointment, AppointmentDTO.class);

        // 🔴 WebSocket update added (Status Change)
        if (removeTime(appointmentDTO.getAppointmentDateTime()).equals(removeTime(new Date()))) {
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
        AppointmentEntity appointmentEntity = appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        String currentDoctorId = appointmentEntity.getDoctorId();
        if (!currentDoctorId.equals(CurrentUserName.getCurrentDoctorId())) {
            throw new ForbiddenException("appointment", "update");
        }

        if (
                (appointmentEntity.getStatus().equals(AppointmentStatus.CANCELLED) || appointmentEntity.getStatus().equals(AppointmentStatus.BOOKED))
                        && !appointmentEntity.getPaymentStatus()
                        && paymentStatus
        ) {
            throw new BusinessRuleException("mark as paid", "Appointment must be in ACCEPTED status");
        }
        appointmentEntity.setPaymentStatus(paymentStatus);
        AppointmentEntity updatedAppointment = appointmentRepository.save(appointmentEntity);

        AppointmentDTO appointmentDTO = modelMapper.map(updatedAppointment, AppointmentDTO.class);

        if (removeTime(appointmentDTO.getAppointmentDateTime()).equals(removeTime(new Date()))) {
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
        AppointmentEntity appointmentEntity = appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));

        String currentDoctorId = appointmentEntity.getDoctorId();
        if (!currentDoctorId.equals(CurrentUserName.getCurrentDoctorId())) {
            throw new ForbiddenException("appointment", "update");
        }

        if (!appointmentEntity.getPaymentStatus()) {
            throw new BusinessRuleException("mark as treated", "Payment is pending");
        }

        if (!appointmentEntity.getAvailableAtClinic()) {
            throw new BusinessRuleException("mark as treated", "Patient is not available at the clinic");
        }

        if (appointmentEntity.getStatus().equals(AppointmentStatus.CANCELLED) || appointmentEntity.getStatus().equals(AppointmentStatus.BOOKED)) {
            throw new BusinessRuleException("mark as treated", "Appointment must be in ACCEPTED status");
        }

        appointmentEntity.setTreatedDateTime(new Date());
        appointmentEntity.setTreated(treatedStatus);

        AppointmentEntity updatedAppointment = appointmentRepository.save(appointmentEntity);

        AppointmentDTO appointmentDTO = modelMapper.map(updatedAppointment, AppointmentDTO.class);
        if (removeTime(appointmentDTO.getAppointmentDateTime()).equals(removeTime(new Date()))) {
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
        AppointmentEntity appointmentEntity = appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));

        String currentDoctorId = appointmentEntity.getDoctorId();
        if (!currentDoctorId.equals(CurrentUserName.getCurrentDoctorId())) {
            throw new ForbiddenException("appointment", "update");
        }

        if (appointmentEntity.getTreated()) {
            throw new BusinessRuleException("update availability", "Patient is already treated");
        }

        if (appointmentEntity.getStatus().equals(AppointmentStatus.CANCELLED) || appointmentEntity.getStatus().equals(AppointmentStatus.BOOKED)) {
            throw new BusinessRuleException("mark as available", "Appointment must be in ACCEPTED status");
        }
        appointmentEntity.setAvailableAtClinic(availableAtClinicStatus);

        AppointmentEntity updatedAppointment = appointmentRepository.save(appointmentEntity);

        AppointmentDTO appointmentDTO = modelMapper.map(updatedAppointment, AppointmentDTO.class);

        if (removeTime(appointmentDTO.getAppointmentDateTime()).equals(removeTime(new Date()))) {
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
        AppointmentEntity appointmentEntity = appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        String currentDoctorId = appointmentEntity.getDoctorId();
        if (!currentDoctorId.equals(CurrentUserName.getCurrentDoctorId())) {
            throw new ForbiddenException("appointment", "cancel");
        }
        if (appointmentEntity.getTreated()) {
            throw new BusinessRuleException("cancel appointment", "Patient is already treated");
        }
        if (Boolean.TRUE.equals(appointmentEntity.getPaymentStatus())) {
            throw new BusinessRuleException("cancel appointment", "Payment has already been received");
        }
        appointmentEntity.setStatus(AppointmentStatus.CANCELLED);

        AppointmentEntity updatedAppointment = appointmentRepository.save(appointmentEntity);

        AppointmentDTO appointmentDTO = modelMapper.map(updatedAppointment, AppointmentDTO.class);

        if (removeTime(appointmentDTO.getAppointmentDateTime()).equals(removeTime(new Date()))) {
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
