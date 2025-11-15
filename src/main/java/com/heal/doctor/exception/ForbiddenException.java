package com.heal.doctor.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BaseException {
    public ForbiddenException(String message) {
        super(message, "FORBIDDEN", HttpStatus.FORBIDDEN);
    }

    public ForbiddenException(String resource, String action) {
        super(
            String.format("Access denied: You are not authorized to %s this %s", action, resource),
            "FORBIDDEN",
            HttpStatus.FORBIDDEN
        );
    }
}

