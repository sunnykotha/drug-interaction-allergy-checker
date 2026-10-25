package com.example.drugchecker;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PrescriptionFinalizationServiceTest {

    private Prescription prescription(boolean withAllergy) {
        Medication existing = new Medication(
                1L, "Warfarin Brand", "warfarin", "anticoagulant", true);
        Medication prescribed = new Medication(
                2L, "NSAID Brand", "nsaid-example", "nsaid", true);

        List<Allergy> allergies = withAllergy
                ? List.of(new Allergy(
                        3L, "nsaid-example", "rash",
                        AllergySeverity.MILD, List.of()))
                : List.of();

        Patient patient = new Patient(
                10L, 45, 70.0, 90.0, false,
                List.of(existing), allergies);

        return new Prescription(
                100L, patient, prescribed, 10.0, "mg",
                LocalDateTime.now());
    }

    private Prescription prescriptionWithCrossReactivity() {
        Medication prescribed = new Medication(
                2L, "NSAID Brand", "nsaid-example", "nsaid", true);

        Allergy allergy = new Allergy(
                4L, "penicillin", "rash",
                AllergySeverity.MILD, List.of("nsaid"));

        Patient patient = new Patient(
                10L, 45, 70.0, 90.0, false,
                List.of(), List.of(allergy));

        return new Prescription(
                101L, patient, prescribed, 10.0, "mg",
                LocalDateTime.now());
    }

    private PrescriptionFinalizationService service(
            Prescription prescription,
            MedicationStatusProvider statusProvider,
            List<InteractionRule> interactionRules,
            List<DoseRangeRule> doseRules,
            AuditTrail auditTrail) {

        return new PrescriptionFinalizationService(
                new InteractionChecker(interactionRules),
                new AllergyChecker(),
                new DoseChecker(doseRules),
                new SeverityEnforcer(),
                auditTrail,
                new MedicationStatusGuard(),
                statusProvider
        );
    }

    @Test
    void finalizesWhenAllChecksPass() {
        Prescription prescription = prescription(false);
        AuditTrail auditTrail = new AuditTrail();

        PrescriptionFinalizationService service = service(
                prescription, id -> true, List.of(), List.of(), auditTrail);

        PrescriptionFinalizationResult result =
                service.finalizePrescription(
                        prescription, UserRole.PHYSICIAN, true,
                        null, false, null, null);

        assertTrue(result.finalized());
        assertTrue(result.interactionAlerts().isEmpty());
        assertTrue(result.allergyAlerts().isEmpty());
        assertTrue(result.doseAlerts().isEmpty());
    }

    @Test
    void blocksMajorInteractionForNurseAndAuditsDecision() {
        Prescription prescription = prescription(false);
        AuditTrail auditTrail = new AuditTrail();

        InteractionRule rule = new InteractionRule(
                "anticoagulant", "nsaid",
                Severity.MAJOR, "Increased bleeding risk");

        PrescriptionFinalizationService service = service(
                prescription, id -> true, List.of(rule), List.of(), auditTrail);

        PrescriptionFinalizationResult result =
                service.finalizePrescription(
                        prescription, UserRole.NURSE, true,
                        "CLINICAL_JUSTIFICATION", false, null, null);

        assertFalse(result.finalized());
        assertEquals(1, result.interactionAlerts().size());
        assertFalse(result.enforcementDecisions().get(0).allowed());
        assertTrue(auditTrail.entriesForPrescription(100L).stream()
                .anyMatch(e -> e.eventType()
                        == AuditEventType.INTERACTION_ALERT));
        assertTrue(auditTrail.entriesForPrescription(100L).stream()
                .anyMatch(e -> e.eventType()
                        == AuditEventType.ENFORCEMENT_DECISION));
    }

    @Test
    void blocksFinalizationWhenAllergyAlertExists() {
        Prescription prescription = prescription(true);
        AuditTrail auditTrail = new AuditTrail();

        PrescriptionFinalizationService service = service(
                prescription, id -> true, List.of(), List.of(), auditTrail);

        PrescriptionFinalizationResult result =
                service.finalizePrescription(
                        prescription, UserRole.PHYSICIAN, true,
                        null, false, null, null);

        assertFalse(result.finalized());
        assertEquals(1, result.allergyAlerts().size());
        assertTrue(auditTrail.entriesForPrescription(100L).stream()
                .anyMatch(e -> e.eventType() == AuditEventType.ALLERGY_ALERT));
    }

    @Test
    void blocksFinalizationWhenDoseAlertExists() {
        Prescription prescription = prescription(false);
        AuditTrail auditTrail = new AuditTrail();

        DoseRangeRule rule = new DoseRangeRule(
                "nsaid-example", "mg",
                1.0, 5.0, 0.0, 0, 120, 0.0, false);

        PrescriptionFinalizationService service = service(
                prescription, id -> true, List.of(), List.of(rule), auditTrail);

        PrescriptionFinalizationResult result =
                service.finalizePrescription(
                        prescription, UserRole.PHYSICIAN, true,
                        null, false, null, null);

        assertFalse(result.finalized());
        assertEquals(1, result.doseAlerts().size());
        assertTrue(auditTrail.entriesForPrescription(100L).stream()
                .anyMatch(e -> e.eventType() == AuditEventType.DOSE_ALERT));
    }

    @Test
    void checksMedicationStatusAgainBeforeReturning() {
        Prescription prescription = prescription(false);
        AtomicInteger checks = new AtomicInteger();

        MedicationStatusProvider statusProvider = id ->
                checks.incrementAndGet() == 1;

        PrescriptionFinalizationService service = service(
                prescription, statusProvider, List.of(), List.of(),
                new AuditTrail());

        assertThrows(
                IllegalStateException.class,
                () -> service.finalizePrescription(
                        prescription, UserRole.PHYSICIAN, true,
                        null, false, null, null)
        );

        assertEquals(2, checks.get());
    }

    @Test
    void rejectsNullPrescription() {
        Prescription sample = prescription(false);

        PrescriptionFinalizationService service = service(
                sample, id -> true, List.of(), List.of(), new AuditTrail());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.finalizePrescription(
                        null, UserRole.PHYSICIAN, true,
                        null, false, null, null)
        );
    }

    @Test
    void allowsAcknowledgedMildCrossReactivityAndAuditsDecision() {
        Prescription prescription = prescriptionWithCrossReactivity();
        AuditTrail auditTrail = new AuditTrail();

        PrescriptionFinalizationService service = service(
                prescription, id -> true, List.of(), List.of(), auditTrail);

        PrescriptionFinalizationResult result =
                service.finalizePrescription(
                        prescription, UserRole.PHYSICIAN, true,
                        null, false, null, null);

        assertTrue(result.finalized());
        assertEquals(1, result.allergyAlerts().size());
        assertEquals(1, result.enforcementDecisions().size());
        assertTrue(result.enforcementDecisions().get(0).allowed());

        List<AuditEntry> entries =
                auditTrail.entriesForPrescription(101L);

        assertTrue(entries.stream().anyMatch(
                e -> e.eventType() == AuditEventType.ALLERGY_ALERT));
        assertTrue(entries.stream().anyMatch(
                e -> e.eventType() == AuditEventType.ENFORCEMENT_DECISION));
    }

    @Test
    void blocksUnacknowledgedMildCrossReactivity() {
        Prescription prescription = prescriptionWithCrossReactivity();
        AuditTrail auditTrail = new AuditTrail();

        PrescriptionFinalizationService service = service(
                prescription, id -> true, List.of(), List.of(), auditTrail);

        PrescriptionFinalizationResult result =
                service.finalizePrescription(
                        prescription, UserRole.PHYSICIAN, false,
                        null, false, null, null);

        assertFalse(result.finalized());
        assertEquals(1, result.allergyAlerts().size());
        assertEquals(1, result.enforcementDecisions().size());
        assertFalse(result.enforcementDecisions().get(0).allowed());

        assertTrue(auditTrail.entriesForPrescription(101L).stream()
                .anyMatch(e -> e.eventType()
                        == AuditEventType.ENFORCEMENT_DECISION));
    }

    @Test
    void directAllergyDecisionIsAuditedEvenWhenAcknowledged() {
        Prescription prescription = prescription(true);
        AuditTrail auditTrail = new AuditTrail();

        PrescriptionFinalizationService service = service(
                prescription, id -> true, List.of(), List.of(), auditTrail);

        PrescriptionFinalizationResult result =
                service.finalizePrescription(
                        prescription, UserRole.PHYSICIAN, true,
                        null, false, null, null);

        assertFalse(result.finalized());
        assertEquals(1, result.allergyAlerts().size());
        assertEquals(1, result.enforcementDecisions().size());
        assertFalse(result.enforcementDecisions().get(0).allowed());

        List<AuditEntry> entries =
                auditTrail.entriesForPrescription(100L);

        assertTrue(entries.stream().anyMatch(
                e -> e.eventType() == AuditEventType.ALLERGY_ALERT));
        assertTrue(entries.stream().anyMatch(
                e -> e.eventType() == AuditEventType.ENFORCEMENT_DECISION));
    }
}