package com.example.drugchecker;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InteractionRulePrescriptionTest {

    @Test
    void interactionMatchesDrugClassesInEitherOrder() {
        InteractionRule rule = new InteractionRule(
                "NSAID", "Anticoagulant",
                Severity.MAJOR, "Increased bleeding risk"
        );

        assertTrue(rule.matches("NSAID", "Anticoagulant"));
        assertTrue(rule.matches("Anticoagulant", "NSAID"));
    }

    @Test
    void interactionNormalizesClassNames() {
        InteractionRule rule = new InteractionRule(
                " NSAID ", " Anticoagulant ",
                Severity.MAJOR, "Increased bleeding risk"
        );

        assertTrue(rule.matches("nsaid", "ANTICOAGULANT"));
        assertEquals("Increased bleeding risk", rule.mechanism());
    }

    @Test
    void interactionDoesNotMatchUnrelatedClasses() {
        InteractionRule rule = new InteractionRule(
                "NSAID", "Anticoagulant",
                Severity.MAJOR, "Increased bleeding risk"
        );

        assertFalse(rule.matches("Antibiotic", "Anticoagulant"));
    }

    @Test
    void rejectsInteractionBetweenSameClass() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InteractionRule(
                        "NSAID", " nsaid ",
                        Severity.MODERATE, "Duplicate therapy"
                )
        );
    }

    @Test
    void rejectsMissingInteractionMechanism() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InteractionRule(
                        "NSAID", "Anticoagulant", Severity.MAJOR, " "
                )
        );
    }

    @Test
    void createsPrescriptionWithValidDetails() {
        Patient patient = new Patient(
                1L, 35, 72.5, 90.0, false, List.of(), List.of()
        );

        Medication medication = new Medication(
                1L, "Brand A", "Generic A", "Analgesic", true
        );

        LocalDateTime prescribedAt = LocalDateTime.of(2026, 10, 25, 10, 0);

        Prescription prescription = new Prescription(
                1L, patient, medication, 250, "mg", prescribedAt
        );

        assertEquals(patient, prescription.patient());
        assertEquals(medication, prescription.medication());
        assertEquals(250, prescription.dose());
        assertEquals("mg", prescription.doseUnit());
        assertEquals(prescribedAt, prescription.prescribedAt());
    }

    @Test
    void rejectsNonPositivePrescriptionDose() {
        Patient patient = new Patient(
                1L, 35, 72.5, 90.0, false, List.of(), List.of()
        );

        Medication medication = new Medication(
                1L, "Brand A", "Generic A", "Analgesic", true
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new Prescription(
                        1L, patient, medication, 0, "mg",
                        LocalDateTime.of(2026, 10, 25, 10, 0)
                )
        );
    }

    @Test
    void rejectsMissingPrescriptionMedication() {
        Patient patient = new Patient(
                1L, 35, 72.5, 90.0, false, List.of(), List.of()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new Prescription(
                        1L, patient, null, 250, "mg",
                        LocalDateTime.of(2026, 10, 25, 10, 0)
                )
        );
    }
}