package com.example.drugchecker;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrescriptionWorkflowTest {

    private Patient patientTakingWarfarin() {
        Medication warfarin = new Medication(
                1L, "Warfarin Brand", "warfarin", "anticoagulant", true
        );

        return new Patient(
                10L,
                55,
                75.0,
                90.0,
                false,
                List.of(warfarin),
                List.of()
        );
    }

    private Prescription nsaidPrescription() {
        Medication nsaid = new Medication(
                2L, "NSAID Brand", "nsaid-example", "nsaid", true
        );

        return new Prescription(
                100L,
                patientTakingWarfarin(),
                nsaid,
                200.0,
                "mg",
                LocalDateTime.now()
        );
    }

    private InteractionChecker checker() {
        InteractionRule rule = new InteractionRule(
                "anticoagulant",
                "nsaid",
                Severity.MAJOR,
                "Increased bleeding risk"
        );

        return new InteractionChecker(List.of(rule));
    }

    @Test
    void detectsMajorInteractionBetweenNsaidAndWarfarin() {
        Prescription prescription = nsaidPrescription();

        List<InteractionAlert> alerts = checker().check(prescription);

        assertEquals(1, alerts.size());
        assertEquals(Severity.MAJOR, alerts.get(0).severity());
        assertEquals(
                "Increased bleeding risk",
                alerts.get(0).mechanism()
        );
        assertEquals(
                InteractionAlertType.DRUG_INTERACTION,
                alerts.get(0).type()
        );
    }

    @Test
    void nurseCannotOverrideMajorInteraction() {
        SeverityEnforcer enforcer = new SeverityEnforcer();

        EnforcementDecision decision = enforcer.evaluate(
                Severity.MAJOR,
                UserRole.NURSE,
                true,
                "CLINICAL_JUSTIFICATION",
                false,
                null,
                null
        );

        assertEquals(EnforcementAction.BLOCK, decision.action());
        assertFalse(decision.allowed());
    }

    @Test
    void physicianMustProvideReasonCodeForMajorInteraction() {
        SeverityEnforcer enforcer = new SeverityEnforcer();

        EnforcementDecision decision = enforcer.evaluate(
                Severity.MAJOR,
                UserRole.PHYSICIAN,
                true,
                null,
                false,
                null,
                null
        );

        assertEquals(
                EnforcementAction.REQUIRE_REASON_CODE,
                decision.action()
        );
        assertFalse(decision.allowed());
    }

    @Test
    void physicianCanOverrideMajorInteractionWithReasonCode() {
        SeverityEnforcer enforcer = new SeverityEnforcer();

        EnforcementDecision decision = enforcer.evaluate(
                Severity.MAJOR,
                UserRole.PHYSICIAN,
                true,
                "CLINICAL_JUSTIFICATION",
                false,
                null,
                null
        );

        assertEquals(
                EnforcementAction.ALLOW_WITH_OVERRIDE,
                decision.action()
        );
        assertTrue(decision.allowed());
    }

    @Test
    void recordsInteractionDecisionAndOverrideForPharmacistReview() {
        Prescription prescription = nsaidPrescription();
        AuditTrail auditTrail = new AuditTrail();

        InteractionAlert alert = checker().check(prescription).get(0);
        auditTrail.recordInteractionAlert(prescription.id(), alert);

        SeverityEnforcer enforcer = new SeverityEnforcer();

        EnforcementDecision nurseDecision = enforcer.evaluate(
                alert.severity(),
                UserRole.NURSE,
                true,
                null,
                false,
                null,
                null
        );
        auditTrail.recordDecision(
                prescription.id(),
                nurseDecision,
                UserRole.NURSE,
                null
        );

        assertFalse(nurseDecision.allowed());

        EnforcementDecision physicianDecision = enforcer.evaluate(
                alert.severity(),
                UserRole.PHYSICIAN,
                true,
                "CLINICAL_JUSTIFICATION",
                false,
                null,
                null
        );
        auditTrail.recordDecision(
                prescription.id(),
                physicianDecision,
                UserRole.PHYSICIAN,
                "CLINICAL_JUSTIFICATION"
        );

        assertTrue(physicianDecision.allowed());

        OverrideRecord override = new OverrideRecord(
                500L,
                prescription.id(),
                Severity.MAJOR,
                UserRole.PHYSICIAN,
                "CLINICAL_JUSTIFICATION",
                "Documented clinical rationale for proceeding",
                false,
                null,
                null,
                LocalDateTime.now()
        );
        auditTrail.recordOverride(override);

        List<AuditEntry> pharmacistReview =
                auditTrail.entriesForPrescription(prescription.id());

        assertEquals(4, pharmacistReview.size());
        assertTrue(pharmacistReview.stream().anyMatch(
                entry -> entry.eventType() == AuditEventType.INTERACTION_ALERT
        ));
        assertTrue(pharmacistReview.stream().anyMatch(
                entry -> entry.eventType() == AuditEventType.OVERRIDE_RECORDED
        ));
        assertTrue(pharmacistReview.stream().anyMatch(
                entry -> entry.actorRole() == UserRole.NURSE
        ));
        assertTrue(pharmacistReview.stream().anyMatch(
                entry -> entry.actorRole() == UserRole.PHYSICIAN
        ));
        assertTrue(pharmacistReview.stream().anyMatch(
                entry -> "CLINICAL_JUSTIFICATION".equals(entry.reasonCode())
        ));
    }
}