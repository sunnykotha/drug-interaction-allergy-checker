package com.example.drugchecker;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContraindicatedWorkflowTest {

    private Prescription prescription() {
        Medication existing = new Medication(
                1L, "Existing Brand", "drug-a", "class-a", true);
        Medication prescribed = new Medication(
                2L, "New Brand", "drug-b", "class-b", true);

        Patient patient = new Patient(
                10L, 45, 70.0, 90.0, false,
                List.of(existing), List.of());

        return new Prescription(
                100L, patient, prescribed, 10.0, "mg",
                LocalDateTime.now());
    }

    private PrescriptionFinalizationService service(AuditTrail auditTrail) {
        InteractionRule rule = new InteractionRule(
                "class-a", "class-b",
                Severity.CONTRAINDICATED,
                "Contraindicated interaction");

        return new PrescriptionFinalizationService(
                new InteractionChecker(List.of(rule)),
                new AllergyChecker(),
                new DoseChecker(List.of()),
                new SeverityEnforcer(),
                auditTrail,
                new MedicationStatusGuard(),
                id -> true
        );
    }

    @Test
    void blocksContraindicatedInteractionWithoutEmergencyDocumentation() {
        Prescription prescription = prescription();

        PrescriptionFinalizationResult result = service(new AuditTrail())
                .finalizePrescription(
                        prescription, UserRole.PHYSICIAN, true,
                        null, false, 200L, UserRole.PHYSICIAN);

        assertFalse(result.finalized());
        assertEquals(
                EnforcementAction.BLOCK,
                result.enforcementDecisions().get(0).action());
    }

    @Test
    void blocksEmergencyWithoutSecondSignOff() {
        Prescription prescription = prescription();

        PrescriptionFinalizationResult result = service(new AuditTrail())
                .finalizePrescription(
                        prescription, UserRole.PHYSICIAN, true,
                        null, true, null, null);

        assertFalse(result.finalized());
        assertEquals(
                EnforcementAction.BLOCK,
                result.enforcementDecisions().get(0).action());
    }

    @Test
    void allowsDocumentedEmergencyWithSecondSignOffAndAuditsOverride() {
        Prescription prescription = prescription();
        AuditTrail auditTrail = new AuditTrail();

        PrescriptionFinalizationResult result = service(auditTrail)
                .finalizePrescription(
                        prescription, 100L, UserRole.PHYSICIAN, true,
                        "EMERGENCY_CLINICAL_JUSTIFICATION",
                        true, 200L, UserRole.PHYSICIAN);

        assertTrue(result.finalized());
        assertEquals(
                EnforcementAction.ALLOW_WITH_OVERRIDE,
                result.enforcementDecisions().get(0).action());

        List<AuditEntry> entries =
                auditTrail.entriesForPrescription(prescription.id());

        assertTrue(entries.stream().anyMatch(
                entry -> entry.eventType() == AuditEventType.OVERRIDE_RECORDED));
        assertTrue(entries.stream().anyMatch(
                entry -> entry.eventType()
                        == AuditEventType.ENFORCEMENT_DECISION));
    }

    @Test
    void blocksDocumentedEmergencyWithoutReasonCode() {
        Prescription prescription = prescription();

        PrescriptionFinalizationResult result = service(new AuditTrail())
                .finalizePrescription(
                        prescription, UserRole.PHYSICIAN, true,
                        null, true, 200L, UserRole.PHYSICIAN);

        assertFalse(result.finalized());
        assertEquals(
                EnforcementAction.REQUIRE_REASON_CODE,
                result.enforcementDecisions().get(0).action());
    }

    @Test
    void automaticallyAuditsMajorInteractionOverride() {
        Medication existing = new Medication(
                1L, "Existing Brand", "drug-a", "class-a", true);
        Medication prescribed = new Medication(
                2L, "New Brand", "drug-b", "class-b", true);
        Patient patient = new Patient(
                10L, 45, 70.0, 90.0, false,
                List.of(existing), List.of());
        Prescription prescription = new Prescription(
                101L, patient, prescribed, 10.0, "mg",
                LocalDateTime.now());

        InteractionRule rule = new InteractionRule(
                "class-a", "class-b", Severity.MAJOR,
                "Major interaction");

        AuditTrail auditTrail = new AuditTrail();
        PrescriptionFinalizationService service =
                new PrescriptionFinalizationService(
                        new InteractionChecker(List.of(rule)),
                        new AllergyChecker(),
                        new DoseChecker(List.of()),
                        new SeverityEnforcer(),
                        auditTrail,
                        new MedicationStatusGuard(),
                        id -> true);

        PrescriptionFinalizationResult result = service.finalizePrescription(
                prescription, UserRole.PHYSICIAN, true,
                "CLINICAL_JUSTIFICATION", false, null, null);

        assertTrue(result.finalized());
        assertTrue(auditTrail.entriesForPrescription(101L).stream()
                .anyMatch(entry ->
                        entry.eventType() == AuditEventType.OVERRIDE_RECORDED));
    }
}