package com.example.drugchecker;

public class MedicationStatusGuard {

    public void ensureActive(
            Medication medication,
            MedicationStatusProvider statusProvider) {

        if (medication == null) {
            throw new IllegalArgumentException("Medication is required");
        }

        if (statusProvider == null) {
            throw new IllegalArgumentException("Medication status provider is required");
        }

        if (!statusProvider.isActive(medication.id())) {
            throw new IllegalStateException(
                    "Medication is discontinued and cannot be finalized");
        }
    }
}