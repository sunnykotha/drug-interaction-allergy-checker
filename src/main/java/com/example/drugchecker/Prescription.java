package com.example.drugchecker;

import java.time.LocalDateTime;

public record Prescription(
        Long id,
        Patient patient,
        Medication medication,
        double dose,
        String doseUnit,
        LocalDateTime prescribedAt
) {
    public Prescription {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Prescription ID must be positive");
        }
        if (patient == null) {
            throw new IllegalArgumentException("Patient is required");
        }
        if (medication == null) {
            throw new IllegalArgumentException("Medication is required");
        }
        if (!Double.isFinite(dose) || dose <= 0) {
            throw new IllegalArgumentException("Dose must be positive and finite");
        }
        if (doseUnit == null || doseUnit.isBlank()) {
            throw new IllegalArgumentException("Dose unit is required");
        }
        if (prescribedAt == null) {
            throw new IllegalArgumentException("Prescription date and time are required");
        }

        doseUnit = doseUnit.trim();
    }
}