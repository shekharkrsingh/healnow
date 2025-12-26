package com.heal.doctor.services;

/**
 * Service interface for generating PDF documents from HTML content.
 */
public interface IPdfService {
    
    /**
     * Generates a PDF as a byte array from the provided HTML content.
     * 
     * @param htmlContent The XHTML compliant content to convert to PDF
     * @return Byte array containing the generated PDF
     * @throws com.heal.doctor.exception.ReportGenerationException if generation fails
     */
    byte[] generatePdfFromHtml(String htmlContent);
}
