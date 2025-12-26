package com.heal.doctor.services;

/**
 * Service interface for generating QR codes
 */
public interface IQRCodeService {
    
    /**
     * Generates a QR code as a Base64 encoded data URL
     * 
     * @param data The data to encode in the QR code
     * @param width The width of the QR code image in pixels
     * @param height The height of the QR code image in pixels
     * @return Base64 encoded data URL string (data:image/png;base64,...)
     * @throws com.heal.doctor.exception.QRCodeGenerationException if QR code generation fails
     */
    String generateQRCodeDataUrl(String data, int width, int height);
    
    /**
     * Generates a QR code as a Base64 encoded data URL with default dimensions (300x300)
     * 
     * @param data The data to encode in the QR code
     * @return Base64 encoded data URL string (data:image/png;base64,...)
     * @throws com.heal.doctor.exception.QRCodeGenerationException if QR code generation fails
     */
    String generateQRCodeDataUrl(String data);
    
    /**
     * Generates a QR code as raw PNG bytes
     * 
     * @param data The data to encode in the QR code
     * @param width The width of the QR code image in pixels
     * @param height The height of the QR code image in pixels
     * @return byte array containing PNG image data
     * @throws com.heal.doctor.exception.QRCodeGenerationException if QR code generation fails
     */
    byte[] generateQRCodeBytes(String data, int width, int height);
}
