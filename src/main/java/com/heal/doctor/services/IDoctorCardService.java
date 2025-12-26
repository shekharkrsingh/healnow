package com.heal.doctor.services;

import java.util.concurrent.CompletableFuture;


public interface IDoctorCardService {

    byte[] generateDoctorCard();
    CompletableFuture<Void> sendDoctorCardByEmail(String doctorId, byte[] pdfBytes);
    CompletableFuture<Void> generateAndSendDoctorCard(String doctorId);
}
