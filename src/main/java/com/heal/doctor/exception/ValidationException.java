package com.heal.doctor.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public class ValidationException extends BaseException {
    private final List<String> validationErrors;

    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        this.validationErrors = List.of(message);
    }

    public ValidationException(String message, List<String> validationErrors) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        this.validationErrors = validationErrors;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}

