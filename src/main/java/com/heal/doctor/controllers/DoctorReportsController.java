package com.heal.doctor.controllers;

import com.heal.doctor.services.IDoctorReports;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


@RestController
@RequestMapping("/api/v1/reports")
public class DoctorReportsController {

    private final IDoctorReports doctorReports;

    public DoctorReportsController(IDoctorReports doctorReports) {
        this.doctorReports = doctorReports;
    }

    @GetMapping(value = "/doctor", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateDoctorReport(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate
    ) {
        byte[] pdfBytes = doctorReports.generateDoctorReport(fromDate, toDate);

        String fileName = String.format("doctor_report_%s_to_%s.pdf",
                (fromDate != null && !fromDate.isBlank() ? fromDate : "start"),
                (toDate != null && !toDate.isBlank() ? toDate : "today")
        );

        String safeFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + safeFileName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
