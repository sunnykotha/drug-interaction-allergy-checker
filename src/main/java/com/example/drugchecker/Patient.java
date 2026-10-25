package com.example.drugchecker;

import java.util.List;

public record Patient(
        Long id,
        int ageYears,
        double weightKg,
        double kidneyFunction,
        boolean pregnant,
        List<Medication> activeMedications,
        List<Allergy> allergies
) {
    public Patient {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Patient ID must be positive");
        }
        if (ageYears < 0) {
            throw new IllegalArgumentException("Age cannot be negative");
        }
        if (!Double.isFinite(weightKg) || weightKg <= 0) {
            throw new IllegalArgumentException("Weight must be positive and finite");
        }
        if (!Double.isFinite(kidneyFunction) || kidneyFunction < 0) {
            throw new IllegalArgumentException(
                    "Kidney function must be non-negative and finite");
        }
        if (activeMedications == null) {
            throw new IllegalArgumentException("Active medications are required");
        }
        if (allergies == null) {
            throw new IllegalArgumentException("Allergies are required");
        }

        activeMedications = List.copyOf(activeMedications);
        allergies = List.copyOf(allergies);
    }
}