package com.heal.doctor.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BaseException {
    public ConflictException(String message) {
        super(message, "CONFLICT", HttpStatus.CONFLICT);
    }

    public ConflictException(String resource, String reason) {
        super(
            String.format("%s already exists. %s", resource, reason),
            "CONFLICT",
            HttpStatus.CONFLICT
        );
    }
}

