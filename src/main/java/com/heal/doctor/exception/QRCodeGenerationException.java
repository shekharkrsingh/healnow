package com.heal.doctor.exception;

/**
 * Exception thrown when QR code generation fails
 */
public class QRCodeGenerationException extends RuntimeException {
    
    public QRCodeGenerationException(String message) {
        super(message);
    }
    
    public QRCodeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
