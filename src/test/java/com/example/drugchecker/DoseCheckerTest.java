package com.example.drugchecker;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DoseCheckerTest {

    private DoseRangeRule rule(
            double minDose,
            double maxDose,
            double maxDosePerKg,
            int minAge,
            int maxAge,
            double minKidneyFunction,
            boolean pregnancyRestricted) {
        return new DoseRangeRule(
                "test-ingredient", "mg",
                minDose, maxDose, maxDosePerKg,
                minAge, maxAge, minKidneyFunction, pregnancyRestricted
        );
    }

    private Prescription prescription(
            int age,
            double weight,
            double kidneyFunction,
            boolean pregnant,
            double dose) {
        Patient patient = new Patient(
                1L, age, weight, kidneyFunction, pregnant, List.of(), List.of()
        );
        Medication medication = new Medication(
                1L, "Test Brand", "test-ingredient", "Test class", true
        );

        return new Prescription(
                1L, patient, medication, dose, "mg",
                LocalDateTime.of(2026, 10, 25, 10, 0)
        );
    }

    @Test
    void acceptsDoseWithinConfiguredLimits() {
        DoseChecker checker = new DoseChecker(
                List.of(rule(10, 500, 0, 18, 100, 30, false))
        );

        assertTrue(checker.check(prescription(35, 70, 90, false, 250)).isEmpty());
    }

    @Test
    void flagsDoseBelowMinimum() {
        DoseChecker checker = new DoseChecker(
                List.of(rule(100, 500, 0, 18, 100, 30, false))
        );

        List<DoseAlert> alerts = checker.check(
                prescription(35, 70, 90, false, 50)
        );

        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).issues().contains(
                "Dose is below the configured minimum"));
    }

    @Test
    void flagsDoseAboveMaximum() {
        DoseChecker checker = new DoseChecker(
                List.of(rule(10, 200, 0, 18, 100, 30, false))
        );

        assertFalse(checker.check(
                prescription(35, 70, 90, false, 250)
        ).isEmpty());
    }

    @Test
    void appliesWeightAdjustedMaximumDose() {
        DoseChecker checker = new DoseChecker(
                List.of(rule(1, 500, 2, 18, 100, 30, false))
        );

        List<DoseAlert> alerts = checker.check(
                prescription(35, 20, 90, false, 50)
        );

        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).issues().contains(
                "Dose exceeds the configured maximum for this patient"));
    }

    @Test
    void flagsPatientOutsideAllowedAgeRange() {
        DoseChecker checker = new DoseChecker(
                List.of(rule(1, 500, 0, 18, 100, 30, false))
        );

        List<DoseAlert> alerts = checker.check(
                prescription(12, 40, 90, false, 100)
        );

        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).issues().contains(
                "Patient age is outside the configured allowed range"));
    }

    @Test
    void flagsLowKidneyFunction() {
        DoseChecker checker = new DoseChecker(
                List.of(rule(1, 500, 0, 18, 100, 60, false))
        );

        List<DoseAlert> alerts = checker.check(
                prescription(35, 70, 25, false, 100)
        );

        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).issues().contains(
                "Kidney function is below the configured threshold"));
    }

    @Test
    void flagsPregnancyRestriction() {
        DoseChecker checker = new DoseChecker(
                List.of(rule(1, 500, 0, 18, 100, 0, true))
        );

        List<DoseAlert> alerts = checker.check(
                prescription(30, 65, 90, true, 100)
        );

        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).issues().contains(
                "Medication is restricted by the configured pregnancy rule"));
    }

    @Test
    void ignoresRulesForDifferentIngredient() {
        DoseChecker checker = new DoseChecker(
                List.of(new DoseRangeRule(
                        "different-ingredient", "mg",
                        1, 10, 0, 18, 100, 0, false
                ))
        );

        assertTrue(checker.check(
                prescription(35, 70, 90, false, 500)
        ).isEmpty());
    }

    @Test
    void resolvesBrandNameWhenMatchingDoseRule() {
        Patient patient = new Patient(
                1L, 35, 70, 90, false, List.of(), List.of()
        );
        Medication medication = new Medication(
                1L, "Mapped Brand", "stored-ingredient", "Test class", true
        );
        Prescription prescription = new Prescription(
                1L, patient, medication, 250, "mg",
                LocalDateTime.of(2026, 10, 25, 10, 0)
        );

        MedicationNameMapper mapper = new MedicationNameMapper(
                java.util.Map.of("Mapped Brand", "mapped-ingredient")
        );
        DoseChecker checker = new DoseChecker(
                List.of(new DoseRangeRule(
                        "mapped-ingredient", "mg",
                        10, 100, 0, 18, 100, 0, false
                )),
                mapper
        );

        List<DoseAlert> alerts = checker.check(prescription);

        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).issues().contains(
                "Dose exceeds the configured maximum for this patient"));
    }
    @Test
    void rejectsNullRules() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DoseChecker(null)
        );
    }

    @Test
    void rejectsNullPrescription() {
        DoseChecker checker = new DoseChecker(List.of());

        assertThrows(
                IllegalArgumentException.class,
                () -> checker.check(null)
        );
    }
}