package com.example.drugchecker;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuditTrailTest {

    private Prescription prescription() {
        Patient patient = new Patient(
                1L, 35, 72.5, 90, false, List.of(), List.of()
        );
        Medication medication = new Medication(
                1L, "Brand A", "ingredient-a", "NSAID", true
        );

        return new Prescription(
                10L, patient, medication, 250, "mg",
                LocalDateTime.of(2026, 10, 25, 10, 0)
        );
    }

    @Test
    void recordsInteractionAlert() {
        InteractionAlert alert = new InteractionAlert(
                prescription().medication(),
                new Medication(2L, "Brand B", "ingredient-b", "Anticoagulant", true),
                InteractionAlertType.DRUG_INTERACTION,
                Severity.MAJOR,
                "Increased bleeding risk"
        );

        AuditTrail trail = new AuditTrail();
        AuditEntry entry = trail.recordInteractionAlert(10L, alert);

        assertEquals(AuditEventType.INTERACTION_ALERT, entry.eventType());
        assertEquals(Severity.MAJOR, entry.severity());
        assertTrue(entry.details().contains("Increased bleeding risk"));
    }

    @Test
    void recordsAllergyAlertAndSeverity() {
        Allergy allergy = new Allergy(
                1L, "ingredient-a", "Anaphylaxis",
                AllergySeverity.LIFE_THREATENING, List.of()
        );
        AllergyAlert alert = new AllergyAlert(
                prescription().medication(),
                allergy,
                AllergyAlertType.DIRECT_ALLERGY,
                "Direct allergen match"
        );

        AuditEntry entry = new AuditTrail().recordAllergyAlert(10L, alert);

        assertEquals(AuditEventType.ALLERGY_ALERT, entry.eventType());
        assertEquals(AllergySeverity.LIFE_THREATENING, entry.allergySeverity());
        assertTrue(entry.details().contains("Anaphylaxis"));
    }

    @Test
    void recordsDoseAlert() {
        DoseAlert alert = new DoseAlert(
                prescription(),
                List.of("Dose exceeds the configured maximum")
        );

        AuditEntry entry = new AuditTrail().recordDoseAlert(10L, alert);

        assertEquals(AuditEventType.DOSE_ALERT, entry.eventType());
        assertTrue(entry.details().contains("configured maximum"));
    }

    @Test
    void recordsBlockedEnforcementDecision() {
        EnforcementDecision decision = new EnforcementDecision(
                EnforcementAction.BLOCK,
                false,
                "A major interaction requires physician authorization"
        );

        AuditEntry entry = new AuditTrail().recordDecision(
                10L, decision, UserRole.NURSE, null
        );

        assertEquals(AuditEventType.ENFORCEMENT_DECISION, entry.eventType());
        assertEquals(UserRole.NURSE, entry.actorRole());
        assertTrue(entry.details().contains("BLOCK"));
    }

    @Test
    void recordsOverrideReasonAndAuthorizingRole() {
        OverrideRecord overrideRecord = new OverrideRecord(
                1L, 10L, Severity.MAJOR, UserRole.PHYSICIAN,
                "BENEFIT_OUTWEIGHS_RISK", "Clinical benefit outweighs risk",
                false, null, null,
                LocalDateTime.of(2026, 10, 25, 10, 0)
        );

        AuditEntry entry = new AuditTrail().recordOverride(overrideRecord);

        assertEquals(AuditEventType.OVERRIDE_RECORDED, entry.eventType());
        assertEquals(UserRole.PHYSICIAN, entry.actorRole());
        assertEquals("BENEFIT_OUTWEIGHS_RISK", entry.reasonCode());
        assertTrue(entry.details().contains("Clinical benefit outweighs risk"));
    }

    @Test
    void filtersAuditEntriesByPrescription() {
        AuditTrail trail = new AuditTrail();
        EnforcementDecision decision = new EnforcementDecision(
                EnforcementAction.ALLOW, true, "Alert acknowledged"
        );

        trail.recordDecision(10L, decision, UserRole.NURSE, null);
        trail.recordDecision(20L, decision, UserRole.PHARMACIST, null);

        assertEquals(1, trail.entriesForPrescription(10L).size());
        assertEquals(2, trail.allEntries().size());
    }

    @Test
    void returnsImmutableAuditEntryLists() {
        AuditTrail trail = new AuditTrail();
        EnforcementDecision decision = new EnforcementDecision(
                EnforcementAction.ALLOW, true, "Alert acknowledged"
        );

        trail.recordDecision(10L, decision, UserRole.NURSE, null);

        assertThrows(
                UnsupportedOperationException.class,
                () -> trail.allEntries().clear()
        );
    }

    @Test
    void assignsUniqueIncreasingAuditIds() {
        AuditTrail trail = new AuditTrail();
        EnforcementDecision decision = new EnforcementDecision(
                EnforcementAction.BLOCK, false, "Prescription blocked"
        );

        AuditEntry first = trail.recordDecision(
                10L, decision, UserRole.NURSE, null
        );
        AuditEntry second = trail.recordDecision(
                10L, decision, UserRole.PHYSICIAN, "REVIEWED"
        );

        assertTrue(second.id() > first.id());
    }

    @Test
    void rejectsInvalidPrescriptionIdWhenQuerying() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuditTrail().entriesForPrescription(0L)
        );
    }

    @Test
    void allowsPharmacistToReviewPrescriptionAuditEntries() {
        AuditTrail trail = new AuditTrail();
        EnforcementDecision decision = new EnforcementDecision(
                EnforcementAction.BLOCK, false, "Major interaction"
        );
        trail.recordDecision(10L, decision, UserRole.PHYSICIAN, "REVIEWED");

        List<AuditEntry> result =
                trail.entriesForPharmacistReview(10L, UserRole.PHARMACIST);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).prescriptionId());
    }

    @Test
    void rejectsNurseFromPharmacistAuditReview() {
        AuditTrail trail = new AuditTrail();

        assertThrows(
                SecurityException.class,
                () -> trail.entriesForPharmacistReview(10L, UserRole.NURSE)
        );
    }

    @Test
    void rejectsNullRoleFromPharmacistAuditReview() {
        AuditTrail trail = new AuditTrail();

        assertThrows(
                IllegalArgumentException.class,
                () -> trail.entriesForPharmacistReview(10L, null)
        );
    }

    @Test
    void rejectsNullOverrideRecord() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuditTrail().recordOverride(null)
        );
    }
}