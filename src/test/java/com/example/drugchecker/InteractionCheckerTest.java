package com.example.drugchecker;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InteractionCheckerTest {

    private Medication medication(
            long id, String name, String ingredient, String drugClass, boolean active) {
        return new Medication(id, name, ingredient, drugClass, active);
    }

    private Prescription prescription(
            Medication prescribed, List<Medication> existing) {
        Patient patient = new Patient(
                1L, 35, 72.5, 90.0, false, existing, List.of()
        );

        return new Prescription(
                1L, patient, prescribed, 250, "mg",
                LocalDateTime.of(2026, 10, 25, 10, 0)
        );
    }

    @Test
    void detectsMajorInteractionWithActiveMedication() {
        Medication prescribed =
                medication(1, "Ibuprofen", "ibuprofen", "NSAID", true);
        Medication existing =
                medication(2, "Warfarin", "warfarin", "Anticoagulant", true);

        InteractionRule rule = new InteractionRule(
                "NSAID", "Anticoagulant",
                Severity.MAJOR, "Increased bleeding risk"
        );

        List<InteractionAlert> alerts = new InteractionChecker(List.of(rule))
                .check(prescription(prescribed, List.of(existing)));

        assertEquals(1, alerts.size());
        assertEquals(Severity.MAJOR, alerts.get(0).severity());
        assertEquals(InteractionAlertType.DRUG_INTERACTION, alerts.get(0).type());
        assertEquals("Increased bleeding risk", alerts.get(0).mechanism());
    }

    @Test
    void checksEveryActiveMedication() {
        Medication prescribed =
                medication(1, "Ibuprofen", "ibuprofen", "NSAID", true);
        Medication warfarin =
                medication(2, "Warfarin", "warfarin", "Anticoagulant", true);
        Medication antibiotic =
                medication(3, "Drug B", "ingredient-b", "Antibiotic", true);

        List<InteractionRule> rules = List.of(
                new InteractionRule(
                        "NSAID", "Anticoagulant",
                        Severity.MAJOR, "Increased bleeding risk"
                ),
                new InteractionRule(
                        "NSAID", "Antibiotic",
                        Severity.MINOR, "Interaction with antibiotic"
                )
        );

        List<InteractionAlert> alerts = new InteractionChecker(rules)
                .check(prescription(prescribed, List.of(warfarin, antibiotic)));

        assertEquals(2, alerts.size());
        assertTrue(alerts.stream().anyMatch(a -> a.severity() == Severity.MAJOR));
        assertTrue(alerts.stream().anyMatch(a -> a.severity() == Severity.MINOR));
    }

    @Test
    void ignoresInactiveExistingMedication() {
        Medication prescribed =
                medication(1, "Ibuprofen", "ibuprofen", "NSAID", true);
        Medication inactive =
                medication(2, "Warfarin", "warfarin", "Anticoagulant", false);

        InteractionRule rule = new InteractionRule(
                "NSAID", "Anticoagulant",
                Severity.MAJOR, "Increased bleeding risk"
        );

        List<InteractionAlert> alerts = new InteractionChecker(List.of(rule))
                .check(prescription(prescribed, List.of(inactive)));

        assertTrue(alerts.isEmpty());
    }

    @Test
    void detectsDuplicateTherapyIgnoringClassCase() {
        Medication prescribed =
                medication(1, "Drug A", "ingredient-a", "NSAID", true);
        Medication existing =
                medication(2, "Drug B", "ingredient-b", " nsaid ", true);

        List<InteractionAlert> alerts = new InteractionChecker(List.of())
                .check(prescription(prescribed, List.of(existing)));

        assertEquals(1, alerts.size());
        assertEquals(InteractionAlertType.DUPLICATE_THERAPY, alerts.get(0).type());
        assertEquals(Severity.MODERATE, alerts.get(0).severity());
    }

    @Test
    void returnsNoAlertsForUnrelatedClasses() {
        Medication prescribed =
                medication(1, "Drug A", "ingredient-a", "NSAID", true);
        Medication existing =
                medication(2, "Drug B", "ingredient-b", "Antibiotic", true);

        InteractionRule rule = new InteractionRule(
                "NSAID", "Anticoagulant",
                Severity.MAJOR, "Increased bleeding risk"
        );

        List<InteractionAlert> alerts = new InteractionChecker(List.of(rule))
                .check(prescription(prescribed, List.of(existing)));

        assertTrue(alerts.isEmpty());
    }

    @Test
    void rejectsNullPrescription() {
        InteractionChecker checker = new InteractionChecker(List.of());

        assertThrows(
                IllegalArgumentException.class,
                () -> checker.check(null)
        );
    }

    @Test
    void rejectsDiscontinuedPrescribedMedication() {
        Medication discontinued =
                medication(1, "Drug A", "ingredient-a", "NSAID", false);

        InteractionChecker checker = new InteractionChecker(List.of());

        assertThrows(
                IllegalArgumentException.class,
                () -> checker.check(prescription(discontinued, List.of()))
        );
    }

    @Test
    void rejectsNullRuleList() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InteractionChecker(null)
        );
    }
}