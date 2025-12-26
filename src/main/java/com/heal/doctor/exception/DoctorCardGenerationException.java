package com.heal.doctor.exception;

/**
 * Exception thrown when doctor card generation fails
 */
public class DoctorCardGenerationException extends RuntimeException {
    
    public DoctorCardGenerationException(String message) {
        super(message);
    }
    
    public DoctorCardGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
