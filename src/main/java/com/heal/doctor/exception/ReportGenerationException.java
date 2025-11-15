package com.heal.doctor.exception;

import org.springframework.http.HttpStatus;

public class ReportGenerationException extends BaseException {
    public ReportGenerationException(String message) {
        super(message, "REPORT_GENERATION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public ReportGenerationException(String message, Throwable cause) {
        super(message, "REPORT_GENERATION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}

