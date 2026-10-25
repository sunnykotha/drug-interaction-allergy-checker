package com.example.drugchecker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DoseChecker {

    private final List<DoseRangeRule> rules;
    private final MedicationNameMapper medicationNameMapper;

    public DoseChecker(List<DoseRangeRule> rules) {
        this(rules, new MedicationNameMapper(Map.of()));
    }

    public DoseChecker(
            List<DoseRangeRule> rules,
            MedicationNameMapper medicationNameMapper) {
        if (rules == null) {
            throw new IllegalArgumentException("Dose rules are required");
        }
        if (rules.stream().anyMatch(rule -> rule == null)) {
            throw new IllegalArgumentException("Dose rules cannot contain null");
        }
        if (medicationNameMapper == null) {
            throw new IllegalArgumentException("Medication name mapper is required");
        }

        this.rules = List.copyOf(rules);
        this.medicationNameMapper = medicationNameMapper;
    }

    public List<DoseAlert> check(Prescription prescription) {
        if (prescription == null) {
            throw new IllegalArgumentException("Prescription is required");
        }
        if (!prescription.medication().active()) {
            throw new IllegalArgumentException(
                    "Cannot check a discontinued prescribed medication");
        }

        String resolvedIngredient = medicationNameMapper
                .resolveGenericIngredient(prescription.medication().brandName())
                .orElse(prescription.medication().genericIngredient());

        List<DoseAlert> alerts = new ArrayList<>();
        Patient patient = prescription.patient();

        for (DoseRangeRule rule : rules) {
            if (!rule.matches(prescription, resolvedIngredient)) {
                continue;
            }

            List<String> issues = new ArrayList<>();

            if (prescription.dose() < rule.minimumDose()) {
                issues.add("Dose is below the configured minimum");
            }

            double effectiveMaximum = rule.maximumDose();

            if (rule.maximumDosePerKg() > 0) {
                effectiveMaximum = Math.min(
                        effectiveMaximum,
                        rule.maximumDosePerKg() * patient.weightKg()
                );
            }

            if (prescription.dose() > effectiveMaximum) {
                issues.add("Dose exceeds the configured maximum for this patient");
            }

            if (patient.ageYears() < rule.minimumAgeYears()
                    || patient.ageYears() > rule.maximumAgeYears()) {
                issues.add("Patient age is outside the configured allowed range");
            }

            if (patient.kidneyFunction() < rule.minimumKidneyFunction()) {
                issues.add("Kidney function is below the configured threshold");
            }

            if (patient.pregnant() && rule.avoidDuringPregnancy()) {
                issues.add("Medication is restricted by the configured pregnancy rule");
            }

            if (!issues.isEmpty()) {
                alerts.add(new DoseAlert(prescription, issues));
            }
        }

        return List.copyOf(alerts);
    }
}