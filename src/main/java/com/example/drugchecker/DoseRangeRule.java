package com.example.drugchecker;

import java.util.Locale;

public record DoseRangeRule(
        String genericIngredient,
        String doseUnit,
        double minimumDose,
        double maximumDose,
        double maximumDosePerKg,
        int minimumAgeYears,
        int maximumAgeYears,
        double minimumKidneyFunction,
        boolean avoidDuringPregnancy
) {
    public DoseRangeRule {
        if (genericIngredient == null || genericIngredient.isBlank()) {
            throw new IllegalArgumentException("Generic ingredient is required");
        }
        if (doseUnit == null || doseUnit.isBlank()) {
            throw new IllegalArgumentException("Dose unit is required");
        }
        if (!Double.isFinite(minimumDose) || minimumDose < 0) {
            throw new IllegalArgumentException("Minimum dose must be non-negative and finite");
        }
        if (!Double.isFinite(maximumDose) || maximumDose <= 0
                || maximumDose < minimumDose) {
            throw new IllegalArgumentException("Maximum dose must be positive and at least the minimum dose");
        }
        if (!Double.isFinite(maximumDosePerKg) || maximumDosePerKg < 0) {
            throw new IllegalArgumentException("Maximum dose per kg must be non-negative and finite");
        }
        if (minimumAgeYears < 0 || maximumAgeYears < minimumAgeYears) {
            throw new IllegalArgumentException("Invalid allowed age range");
        }
        if (!Double.isFinite(minimumKidneyFunction) || minimumKidneyFunction < 0) {
            throw new IllegalArgumentException("Minimum kidney function must be non-negative and finite");
        }

        genericIngredient = genericIngredient.trim().toLowerCase(Locale.ROOT);
        doseUnit = doseUnit.trim().toLowerCase(Locale.ROOT);
    }

    public boolean matches(Prescription prescription) {
        return matches(prescription, prescription.medication().genericIngredient());
    }

    public boolean matches(
            Prescription prescription,
            String resolvedGenericIngredient) {
        return resolvedGenericIngredient != null
                && resolvedGenericIngredient.trim().toLowerCase(Locale.ROOT)
                        .equals(genericIngredient)
                && prescription.doseUnit().trim().toLowerCase(Locale.ROOT)
                        .equals(doseUnit);
    }
}