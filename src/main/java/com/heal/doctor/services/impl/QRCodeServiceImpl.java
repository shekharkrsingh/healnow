package com.heal.doctor.services.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.heal.doctor.exception.QRCodeGenerationException;
import com.heal.doctor.services.IQRCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class QRCodeServiceImpl implements IQRCodeService {

    private static final Logger logger = LoggerFactory.getLogger(QRCodeServiceImpl.class);
    private static final int DEFAULT_QR_CODE_SIZE = 300;
    private static final String IMAGE_FORMAT = "PNG";

    @Override
    public String generateQRCodeDataUrl(String data, int width, int height) {
        logger.debug("Generating QR code data URL for data: {} with dimensions {}x{}", data, width, height);
        try {
            byte[] qrCodeBytes = generateQRCodeBytes(data, width, height);
            String base64Image = Base64.getEncoder().encodeToString(qrCodeBytes);
            String dataUrl = "data:image/png;base64," + base64Image;
            logger.info("Successfully generated QR code data URL");
            return dataUrl;
        } catch (Exception e) {
            logger.error("Failed to generate QR code data URL: {}", e.getMessage(), e);
            throw new QRCodeGenerationException("Failed to generate QR code data URL", e);
        }
    }

    @Override
    public String generateQRCodeDataUrl(String data) {
        return generateQRCodeDataUrl(data, DEFAULT_QR_CODE_SIZE, DEFAULT_QR_CODE_SIZE);
    }

    @Override
    public byte[] generateQRCodeBytes(String data, int width, int height) {
        logger.debug("Generating QR code bytes for data: {} with dimensions {}x{}", data, width, height);
        
        if (data == null || data.trim().isEmpty()) {
            throw new QRCodeGenerationException("QR code data cannot be null or empty");
        }
        
        if (width <= 0 || height <= 0) {
            throw new QRCodeGenerationException("QR code dimensions must be positive");
        }

        try {
            // Configure QR code encoding hints
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            // Generate QR code matrix
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height, hints);

            // Convert matrix to buffered image
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            // Convert image to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(qrImage, IMAGE_FORMAT, outputStream);
            byte[] qrCodeBytes = outputStream.toByteArray();

            logger.info("Successfully generated QR code bytes. Size: {} bytes", qrCodeBytes.length);
            return qrCodeBytes;
            
        } catch (WriterException e) {
            logger.error("Failed to encode QR code: {}", e.getMessage(), e);
            throw new QRCodeGenerationException("Failed to encode QR code", e);
        } catch (IOException e) {
            logger.error("Failed to write QR code image: {}", e.getMessage(), e);
            throw new QRCodeGenerationException("Failed to write QR code image", e);
        }
    }
}
