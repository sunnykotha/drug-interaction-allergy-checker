package com.example.drugchecker;

public record AllergyAlert(
        Medication prescribedMedication,
        Allergy allergy,
        AllergyAlertType type,
        String explanation
) {
    public AllergyAlert {
        if (prescribedMedication == null) {
            throw new IllegalArgumentException("Prescribed medication is required");
        }
        if (allergy == null) {
            throw new IllegalArgumentException("Allergy is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("Allergy alert type is required");
        }
        if (explanation == null || explanation.isBlank()) {
            throw new IllegalArgumentException("Explanation is required");
        }

        explanation = explanation.trim();
    }
}