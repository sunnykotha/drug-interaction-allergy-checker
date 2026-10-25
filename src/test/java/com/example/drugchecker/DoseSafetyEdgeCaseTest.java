package com.example.drugchecker;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DoseSafetyEdgeCaseTest {

    private Prescription prescription(
            int age,
            double weightKg,
            double kidneyFunction,
            boolean pregnant,
            double dose,
            String unit) {

        Patient patient = new Patient(
                1L,
                age,
                weightKg,
                kidneyFunction,
                pregnant,
                List.of(),
                List.of()
        );

        Medication medication = new Medication(
                10L,
                "Brand A",
                "generic-a",
                "class-a",
                true
        );

        return new Prescription(
                100L,
                patient,
                medication,
                dose,
                unit,
                LocalDateTime.now()
        );
    }

    private DoseRangeRule rule(
            double minimumDose,
            double maximumDose,
            double maximumDosePerKg,
            int minimumAge,
            int maximumAge,
            double minimumKidneyFunction,
            boolean avoidDuringPregnancy) {

        return new DoseRangeRule(
                "generic-a",
                "mg",
                minimumDose,
                maximumDose,
                maximumDosePerKg,
                minimumAge,
                maximumAge,
                minimumKidneyFunction,
                avoidDuringPregnancy
        );
    }

    @Test
    void detectsPrescriptionOutsideConfiguredPediatricAgeRange() {
        Prescription prescription = prescription(
                10, 30.0, 90.0, false, 5.0, "mg"
        );

        DoseRangeRule rule = rule(
                1.0, 10.0, 0.0, 18, 120, 0.0, false
        );

        assertFalse(new DoseChecker(List.of(rule))
                .check(prescription).isEmpty());
    }

    @Test
    void acceptsPrescriptionWithinConfiguredPediatricAgeRange() {
        Prescription prescription = prescription(
                10, 30.0, 90.0, false, 5.0, "mg"
        );

        DoseRangeRule rule = rule(
                1.0, 10.0, 0.0, 2, 17, 0.0, false
        );

        assertTrue(new DoseChecker(List.of(rule))
                .check(prescription).isEmpty());
    }

    @Test
    void detectsDoseAboveConfiguredWeightBasedMaximum() {
        Prescription prescription = prescription(
                10, 20.0, 90.0, false, 50.0, "mg"
        );

        DoseRangeRule rule = rule(
                1.0, 100.0, 2.0, 0, 120, 0.0, false
        );

        assertFalse(new DoseChecker(List.of(rule))
                .check(prescription).isEmpty());
    }

    @Test
    void acceptsDoseAtConfiguredWeightBasedMaximum() {
        Prescription prescription = prescription(
                10, 20.0, 90.0, false, 40.0, "mg"
        );

        DoseRangeRule rule = rule(
                1.0, 100.0, 2.0, 0, 120, 0.0, false
        );

        assertTrue(new DoseChecker(List.of(rule))
                .check(prescription).isEmpty());
    }

    @Test
    void detectsConfiguredPregnancyRestriction() {
        Prescription prescription = prescription(
                30, 65.0, 90.0, true, 5.0, "mg"
        );

        DoseRangeRule rule = rule(
                1.0, 10.0, 0.0, 0, 120, 0.0, true
        );

        assertFalse(new DoseChecker(List.of(rule))
                .check(prescription).isEmpty());
    }

    @Test
    void acceptsNonPregnantPatientWhenPregnancyIsOnlyRestriction() {
        Prescription prescription = prescription(
                30, 65.0, 90.0, false, 5.0, "mg"
        );

        DoseRangeRule rule = rule(
                1.0, 10.0, 0.0, 0, 120, 0.0, true
        );

        assertTrue(new DoseChecker(List.of(rule))
                .check(prescription).isEmpty());
    }

    @Test
    void detectsKidneyFunctionBelowConfiguredMinimum() {
        Prescription prescription = prescription(
                40, 70.0, 20.0, false, 5.0, "mg"
        );

        DoseRangeRule rule = rule(
                1.0, 10.0, 0.0, 0, 120, 60.0, false
        );

        assertFalse(new DoseChecker(List.of(rule))
                .check(prescription).isEmpty());
    }

    @Test
    void acceptsKidneyFunctionAtConfiguredMinimum() {
        Prescription prescription = prescription(
                40, 70.0, 60.0, false, 5.0, "mg"
        );

        DoseRangeRule rule = rule(
                1.0, 10.0, 0.0, 0, 120, 60.0, false
        );

        assertTrue(new DoseChecker(List.of(rule))
                .check(prescription).isEmpty());
    }

    @Test
    void detectsDoseAboveConfiguredMaximum() {
        Prescription prescription = prescription(
                40, 70.0, 90.0, false, 11.0, "mg"
        );

        DoseRangeRule rule = rule(
                1.0, 10.0, 0.0, 0, 120, 0.0, false
        );

        assertFalse(new DoseChecker(List.of(rule))
                .check(prescription).isEmpty());
    }

    @Test
    void acceptsDoseAtConfiguredMaximum() {
        Prescription prescription = prescription(
                40, 70.0, 90.0, false, 10.0, "mg"
        );

        DoseRangeRule rule = rule(
                1.0, 10.0, 0.0, 0, 120, 0.0, false
        );

        assertTrue(new DoseChecker(List.of(rule))
                .check(prescription).isEmpty());
    }
}