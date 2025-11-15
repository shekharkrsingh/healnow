package com.heal.doctor.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BaseException {
    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException() {
        super("Authentication required", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
    }
}

