package com.heal.doctor.services.impl;

import com.heal.doctor.dto.AppointmentDTO;
import com.heal.doctor.dto.DoctorDTO;
import com.heal.doctor.exception.BadRequestException;
import com.heal.doctor.exception.ReportGenerationException;
import com.heal.doctor.services.IAppointmentService;
import com.heal.doctor.services.IDoctorReports;
import com.heal.doctor.services.IDoctorService;
import com.heal.doctor.services.IEmailService;
import com.heal.doctor.utils.CurrentUserName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DoctorReportsImpl implements IDoctorReports {

    private final TemplateEngine templateEngine;
    private final IDoctorService doctorService;
    private final IAppointmentService appointmentService;
    private final IEmailService emailService;

    @Value("${company.name}")
    private String companyName;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd MMM, yyyy");

    public DoctorReportsImpl(TemplateEngine templateEngine,
                             IDoctorService doctorService,
                             IAppointmentService appointmentService, IEmailService emailService) {
        this.templateEngine = templateEngine;
        this.doctorService = doctorService;
        this.appointmentService = appointmentService;
        this.emailService = emailService;
    }

    @Override
    public byte[] generateDoctorReport(String fromDate, String toDate) {
        try {
            String finalFromDate = validateOrDefaultFromDate(fromDate);
            String finalToDate = validateOrDefaultToDate(toDate);

            DoctorDTO doctor = doctorService.getDoctorProfile();

            List<AppointmentDTO> appointments = appointmentService.getAppointmentsByDoctorAndDateRange(
                    doctor.getDoctorId(),
                    finalFromDate,
                    finalToDate
            );

            Map<String, Object> variables = new HashMap<>();
            variables.put("companyName", companyName);
            variables.put("doctorName", doctor.getFirstName() + " " + doctor.getLastName());
            variables.put("doctorId", doctor.getDoctorId());
            variables.put("specialization", doctor.getSpecialization());
            variables.put("address", doctor.getClinicAddress());
            variables.put("reportFromDate", LocalDate.parse(finalFromDate, FORMATTER).format(DISPLAY_FORMATTER));
            variables.put("reportToDate", LocalDate.parse(finalToDate, FORMATTER).format(DISPLAY_FORMATTER));
            variables.put("reportGeneratedOn", LocalDate.now().format(DISPLAY_FORMATTER));
            variables.put("appointments", appointments);
            variables.put("totalAppointments", appointments.size());
            
            Map<String, Long> appointmentStats = appointments.stream()
                    .collect(Collectors.groupingBy(
                            a -> {
                                if (Boolean.TRUE.equals(a.getTreated())) return "treated";
                                if (a.getStatus() != null && a.getStatus().name().equals("CANCELLED")) return "cancelled";
                                return "other";
                            },
                            Collectors.counting()
                    ));
            
            variables.put("treatedAppointments", appointmentStats.getOrDefault("treated", 0L));
            variables.put("cancelledAppointments", appointmentStats.getOrDefault("cancelled", 0L));

            Context context = new Context();
            context.setVariables(variables);

            String htmlContent = templateEngine.process("doctor-report-template", context);

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                ITextRenderer renderer = new ITextRenderer();
                renderer.setDocumentFromString(htmlContent);
                renderer.layout();
                renderer.createPDF(outputStream);

                emailService.sendSimpleEmailWithAttachment(
                        CurrentUserName.getCurrentUsername(),
                        "Doctor Appointment Report - " + LocalDate.now().format(DISPLAY_FORMATTER),
                        "Please find your appointment report attached.",
                        outputStream.toByteArray(),
                        "appointment-report-" + LocalDate.now() + ".pdf",
                        "application/pdf"
                );

                return outputStream.toByteArray();
            }

        } catch (BadRequestException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new ReportGenerationException("Failed to generate PDF report: " + e.getMessage(), e);
        }
    }

    private String validateOrDefaultFromDate(String fromDate) {
        if (fromDate == null || fromDate.isBlank()) {
            return LocalDate.now().withDayOfMonth(1).format(FORMATTER);
        }
        try {
            LocalDate.parse(fromDate, FORMATTER);
            return fromDate;
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid fromDate format. Expected yyyy-MM-dd, got: " + fromDate);
        }
    }

    private String validateOrDefaultToDate(String toDate) {
        if (toDate == null || toDate.isBlank()) {
            return LocalDate.now().minusDays(1).format(FORMATTER);
        }
        try {
            LocalDate.parse(toDate, FORMATTER);
            return toDate;
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid toDate format. Expected yyyy-MM-dd, got: " + toDate);
        }
    }
}