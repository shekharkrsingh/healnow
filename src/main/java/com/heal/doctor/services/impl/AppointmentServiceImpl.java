package com.heal.doctor.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heal.doctor.dto.AppointmentDTO;
import com.heal.doctor.dto.AppointmentRequestDTO;
import com.heal.doctor.models.AppointmentEntity;
import com.heal.doctor.models.enums.AppointmentStatus;
import com.heal.doctor.models.enums.AppointmentType;
import com.heal.doctor.repositories.AppointmentRepository;
import com.heal.doctor.services.IAppointmentService;
import com.heal.doctor.utils.AppointmentId;
import com.heal.doctor.utils.CurrentUserName;
import com.heal.doctor.utils.DateUtils;
import com.heal.doctor.websocket.AppointmentWebSocketHandler;
import org.modelmapper.ModelMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class AppointmentServiceImpl implements IAppointmentService {


    private final AppointmentRepository appointmentRepository;
    private final AppointmentWebSocketHandler webSocketHandler;
    private final ModelMapper modelMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  AppointmentWebSocketHandler webSocketHandler,
                                  ModelMapper modelMapper, SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper)
    {
        this.appointmentRepository = appointmentRepository;
        this.webSocketHandler = webSocketHandler;
        this.modelMapper = modelMapper;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @Override
    public AppointmentDTO bookAppointment(AppointmentRequestDTO requestDTO) {
        if (requestDTO.getPatientName() == null || requestDTO.getPatientName().trim().isEmpty()) {
            throw new IllegalArgumentException("Patient Name is required and cannot be empty.");
        }

        if (requestDTO.getPaymentStatus() == null) {
            throw new IllegalArgumentException("Payment Status is required.");
        }

        if (requestDTO.getAvailableAtClinic() == null) {
            throw new IllegalArgumentException("Availability at clinic is required.");
        }

        if (requestDTO.getContact() == null || requestDTO.getContact().trim().isEmpty()) {
            throw new IllegalArgumentException("Contact is required and cannot be empty.");
        }

        if (requestDTO.getContact().trim().length() != 10) {
            throw new IllegalArgumentException("Not a valid contact no.");
        }

        String doctorId = CurrentUserName.getCurrentDoctorId();
        Date appointmentDate = requestDTO.getBookingDateTime() != null ? requestDTO.getBookingDateTime() : new Date();
        Date[] date = DateUtils.getStartAndEndOfDay(new Date());

        boolean exists = appointmentRepository.existsByDoctorIdAndPatientNameAndContactAndBookingDateTimeBetween(
                doctorId,
                requestDTO.getPatientName(),
                requestDTO.getContact(),
                date[0],
                date[1]);

        if (exists) {
            throw new RuntimeException("An appointment for this doctor already exists on the selected date.");
        }

        AppointmentEntity appointmentEntity = modelMapper.map(requestDTO, AppointmentEntity.class);
        appointmentEntity.setStatus(AppointmentStatus.ACCEPTED);
        appointmentEntity.setAppointmentDateTime(appointmentDate);
        appointmentEntity.setBookingDateTime(appointmentDate);
        appointmentEntity.setDoctorId(doctorId);
        appointmentEntity.setAppointmentId(AppointmentId.generateAppointmentId(doctorId));
        appointmentEntity.setTreated(false);
        appointmentEntity.setAppointmentType(AppointmentType.IN_PERSON);

        AppointmentEntity savedAppointment = appointmentRepository.save(appointmentEntity);
        System.out.println("Sending WebSocket update for appointment: " + savedAppointment.getAppointmentId());

        // **Send Real-Time WebSocket Update**
        messagingTemplate.convertAndSend("/topic/appointments", modelMapper.map(savedAppointment, AppointmentDTO.class));


        System.out.println(savedAppointment);
        return modelMapper.map(savedAppointment, AppointmentDTO.class);
    }





    @Override
    public AppointmentDTO getAppointmentById(String appointmentId) {
        AppointmentEntity appointmentEntity=appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(()->new RuntimeException("Appointment not found"));
        String currentDoctorId=appointmentEntity.getDoctorId();
        if(!currentDoctorId.equals(CurrentUserName.getCurrentDoctorId())){
            throw new SecurityException("Access denied: You are not authorized to view this appointment.");
        }
        return  modelMapper.map(appointmentEntity, AppointmentDTO.class);
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

    @Override
    public AppointmentDTO updateAppointmentStatus(String appointmentId, AppointmentStatus status) {
        AppointmentEntity appointmentEntity=appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(()->new RuntimeException("Appointment not found"));
        String currentDoctorId=appointmentEntity.getDoctorId();
        if(!currentDoctorId.equals(CurrentUserName.getCurrentDoctorId())){
            throw new SecurityException("Access denied: You are not authorized to update this appointment.");
        }
        appointmentEntity.setStatus(status);
        AppointmentEntity updatedAppointment = appointmentRepository.save(appointmentEntity);

        // 🔴 WebSocket update added (Status Change)
        messagingTemplate.convertAndSend("/topic/appointments", modelMapper.map(updatedAppointment, AppointmentDTO.class));

        return modelMapper.map(updatedAppointment, AppointmentDTO.class);
    }


    @Override
    public AppointmentDTO updatePaymentStatus(String appointmentId, Boolean paymentStatus) {
        AppointmentEntity appointmentEntity=appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(()->new RuntimeException("Appointment not found"));
        String currentDoctorId=appointmentEntity.getDoctorId();
        if(!currentDoctorId.equals(CurrentUserName.getCurrentDoctorId())){
            throw new SecurityException("Access denied: You are not authorized to update this appointment.");
        }

        if(
                (appointmentEntity.getStatus().equals(AppointmentStatus.CANCELLED) || appointmentEntity.getStatus().equals(AppointmentStatus.BOOKED))
                && !appointmentEntity.getPaymentStatus()
                && paymentStatus
        ) {
            throw new RuntimeException("Cannot mark as paid: Appointment is not marked as accepted");
            }
        appointmentEntity.setPaymentStatus(paymentStatus);
        AppointmentEntity updatedAppointement=appointmentRepository.save(appointmentEntity);

            messagingTemplate.convertAndSend("/topic/appointments", modelMapper.map(updatedAppointement, AppointmentDTO.class));

        return modelMapper.map(appointmentRepository.save(appointmentEntity),AppointmentDTO.class);
    }




    @Override
    public AppointmentDTO updateTreatedStatus(String appointmentId, Boolean treatedStatus) {
        AppointmentEntity appointmentEntity = appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        String currentDoctorId=appointmentEntity.getDoctorId();
        if(!currentDoctorId.equals(CurrentUserName.getCurrentDoctorId())){
            throw new SecurityException("Access denied: You are not authorized to view this appointment.");
        }

        if (!appointmentEntity.getPaymentStatus()) {
            throw new RuntimeException("Cannot mark as treated: Payment is pending.");
        }

        if (!appointmentEntity.getAvailableAtClinic()) {
            throw new RuntimeException("Cannot mark as treated: Patient is not available at the clinic.");
        }

        if (appointmentEntity.getStatus().equals(AppointmentStatus.CANCELLED) || appointmentEntity.getStatus().equals(AppointmentStatus.BOOKED)) {
            throw new RuntimeException("Cannot mark as treated: Appointment is not marked as accepted.");
        }

        appointmentEntity.setTreatedDateTime(new Date());
        appointmentEntity.setTreated(treatedStatus);

        AppointmentEntity updatedAppointment = appointmentRepository.save(appointmentEntity);

        // 🔴 WebSocket update added (Treated Status Change)
        messagingTemplate.convertAndSend("/topic/appointments", modelMapper.map(updatedAppointment, AppointmentDTO.class));

        return modelMapper.map(appointmentRepository.save(appointmentEntity), AppointmentDTO.class);
    }


    @Override
    public AppointmentDTO updateAvailableAtClinic(String appointmentId, Boolean availableAtClinicStatus) {
        AppointmentEntity appointmentEntity=appointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(()->new RuntimeException("Appointment not found"));

        String currentDoctorId=appointmentEntity.getDoctorId();
        if(!currentDoctorId.equals(CurrentUserName.getCurrentDoctorId())){
            throw new SecurityException("Access denied: You are not authorized to view this appointment.");
        }

        if (appointmentEntity.getTreated()) {
            throw new RuntimeException("Cannot update availability: Patient is already treated.");
        }


        if (appointmentEntity.getStatus().equals(AppointmentStatus.CANCELLED) || appointmentEntity.getStatus().equals(AppointmentStatus.BOOKED)) {
            throw new RuntimeException("Cannot mark as available: Appointment is not marked as accepted.");
        }
        appointmentEntity.setAvailableAtClinic(availableAtClinicStatus);

        AppointmentEntity updatedAppointment = appointmentRepository.save(appointmentEntity);

        // 🔴 WebSocket update added (Clinic Availability Change)
        messagingTemplate.convertAndSend("/topic/appointments", modelMapper.map(updatedAppointment, AppointmentDTO.class));


        return modelMapper.map(appointmentRepository.save(appointmentEntity),AppointmentDTO.class);
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
