package com.heal.doctor.services.impl;

import com.heal.doctor.exception.ReportGenerationException;
import com.heal.doctor.services.IPdfService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.PdfAConformance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfServiceImpl implements IPdfService {

    private static final Logger logger = LoggerFactory.getLogger(PdfServiceImpl.class);

    @Override
    public byte[] generatePdfFromHtml(String htmlContent) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.usePdfUaAccessbility(false);
            builder.usePdfAConformance(PdfAConformance.NONE);
            builder.withHtmlContent(htmlContent, null);
            builder.toStream(outputStream);
            builder.run();

            byte[] pdfBytes = outputStream.toByteArray();
            logger.info("PDF generated successfully, size: {} bytes", pdfBytes.length);
            return pdfBytes;
        } catch (Exception e) {
            logger.error("Failed to generate PDF from HTML: {}", e.getMessage(), e);
            throw new ReportGenerationException("Failed to generate PDF from HTML", e);
        }
    }
}
