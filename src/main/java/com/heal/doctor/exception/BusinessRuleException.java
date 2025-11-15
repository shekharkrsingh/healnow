package com.heal.doctor.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends BaseException {
    public BusinessRuleException(String message) {
        super(message, "BUSINESS_RULE_VIOLATION", HttpStatus.BAD_REQUEST);
    }

    public BusinessRuleException(String rule, String reason) {
        super(
            String.format("Cannot %s: %s", rule, reason),
            "BUSINESS_RULE_VIOLATION",
            HttpStatus.BAD_REQUEST
        );
    }
}

