package com.example.drugchecker;

public record InteractionAlert(
        Medication prescribedMedication,
        Medication existingMedication,
        InteractionAlertType type,
        Severity severity,
        String mechanism
) {
    public InteractionAlert {
        if (prescribedMedication == null) {
            throw new IllegalArgumentException("Prescribed medication is required");
        }
        if (existingMedication == null) {
            throw new IllegalArgumentException("Existing medication is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("Alert type is required");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Alert severity is required");
        }
        if (mechanism == null || mechanism.isBlank()) {
            throw new IllegalArgumentException("Alert explanation is required");
        }

        mechanism = mechanism.trim();
    }
}