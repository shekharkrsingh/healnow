package com.heal.doctor.controllers;

import com.heal.doctor.services.IDoctorCardService;
import com.heal.doctor.utils.CurrentUserName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/doctor/card")
@PreAuthorize("hasAnyRole('DOCTOR')")
public class DoctorCardController {

    private static final Logger logger = LoggerFactory.getLogger(DoctorCardController.class);

    private final IDoctorCardService doctorCardService;

    @Autowired
    public DoctorCardController(IDoctorCardService doctorCardService) {
        this.doctorCardService = doctorCardService;
    }
    @PostMapping("/generate-and-send")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> generateAndSendDoctorCard() {

        String doctorId = CurrentUserName.getCurrentDoctorId();
        logger.info("Received request to generate and send doctor card for doctorId: {}", doctorId);

        try {
            byte[] pdfBytes = doctorCardService.generateDoctorCard();

            logger.info("Doctor card generated successfully for doctorId: {}. PDF size: {} bytes", 
                    doctorId, pdfBytes.length);

            String dynamicFilename = String.format("appointment-booking-card-%s.pdf", doctorId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", dynamicFilename);
            headers.setContentLength(pdfBytes.length);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);
            headers.add("X-Email-Status", "Email sending initiated");

            logger.info("Successfully returned doctor card PDF for doctorId: {}", doctorId);
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Failed to generate and send doctor card for doctorId: {}", doctorId, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
