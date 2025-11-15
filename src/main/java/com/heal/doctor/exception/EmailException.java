package com.heal.doctor.exception;

import org.springframework.http.HttpStatus;

public class EmailException extends BaseException {
    public EmailException(String message) {
        super(message, "EMAIL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public EmailException(String message, Throwable cause) {
        super(message, "EMAIL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}

