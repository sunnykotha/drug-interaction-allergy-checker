package com.example.drugchecker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeverityEnforcerTest {

    private final SeverityEnforcer enforcer = new SeverityEnforcer();

    @Test
    void minorAlertRequiresAcknowledgment() {
        EnforcementDecision decision = enforcer.evaluate(
                Severity.MINOR, UserRole.NURSE,
                false, null, false, null, null
        );

        assertEquals(EnforcementAction.REQUIRE_ACKNOWLEDGMENT, decision.action());
        assertFalse(decision.allowed());
    }

    @Test
    void acknowledgedMinorAlertCanProceed() {
        EnforcementDecision decision = enforcer.evaluate(
                Severity.MINOR, UserRole.NURSE,
                true, null, false, null, null
        );

        assertEquals(EnforcementAction.ALLOW, decision.action());
        assertTrue(decision.allowed());
    }

    @Test
    void moderateAlertRequiresAcknowledgment() {
        EnforcementDecision decision = enforcer.evaluate(
                Severity.MODERATE, UserRole.PHARMACIST,
                false, null, false, null, null
        );

        assertEquals(EnforcementAction.REQUIRE_ACKNOWLEDGMENT, decision.action());
        assertFalse(decision.allowed());
    }

    @Test
    void acknowledgedModerateAlertCanProceed() {
        EnforcementDecision decision = enforcer.evaluate(
                Severity.MODERATE, UserRole.PHARMACIST,
                true, null, false, null, null
        );

        assertEquals(EnforcementAction.ALLOW, decision.action());
        assertTrue(decision.allowed());
    }

    @Test
    void nurseCannotOverrideMajorInteraction() {
        EnforcementDecision decision = enforcer.evaluate(
                Severity.MAJOR, UserRole.NURSE,
                false, "CLINICAL_REASON", false, null, null
        );

        assertEquals(EnforcementAction.BLOCK, decision.action());
        assertFalse(decision.allowed());
    }

    @Test
    void physicianMustProvideReasonCodeForMajorInteraction() {
        EnforcementDecision decision = enforcer.evaluate(
                Severity.MAJOR, UserRole.PHYSICIAN,
                false, " ", false, null, null
        );

        assertEquals(EnforcementAction.REQUIRE_REASON_CODE, decision.action());
        assertFalse(decision.allowed());
    }

    @Test
    void physicianCanOverrideMajorInteractionWithReasonCode() {
        EnforcementDecision decision = enforcer.evaluate(
                Severity.MAJOR, UserRole.PHYSICIAN,
                false, "BENEFIT_OUTWEIGHS_RISK", false, null, null
        );

        assertEquals(EnforcementAction.ALLOW_WITH_OVERRIDE, decision.action());
        assertTrue(decision.allowed());
    }

    @Test
    void contraindicatedInteractionIsBlockedWithoutEmergencyDocumentation() {
        EnforcementDecision decision = enforcer.evaluate(
                Severity.CONTRAINDICATED, UserRole.PHYSICIAN,
                false, "EMERGENCY", false, 99L, UserRole.PHARMACIST
        );

        assertEquals(EnforcementAction.BLOCK, decision.action());
        assertFalse(decision.allowed());
    }

    @Test
    void contraindicatedInteractionIsBlockedWithoutSecondSignOff() {
        EnforcementDecision decision = enforcer.evaluate(
                Severity.CONTRAINDICATED, UserRole.PHYSICIAN,
                false, "EMERGENCY", true, null, null
        );

        assertEquals(EnforcementAction.BLOCK, decision.action());
        assertFalse(decision.allowed());
    }

    @Test
    void documentedEmergencyWithSecondSignOffCanProceed() {
        EnforcementDecision decision = enforcer.evaluate(
                Severity.CONTRAINDICATED, UserRole.PHYSICIAN,
                false, "EMERGENCY", true, 99L, UserRole.PHARMACIST,
                100L
        );

        assertEquals(EnforcementAction.ALLOW_WITH_OVERRIDE, decision.action());
        assertTrue(decision.allowed());
    }

    @Test
    void rejectsMissingSeverity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> enforcer.evaluate(
                        null, UserRole.PHYSICIAN,
                        false, null, false, null, null
                )
        );
    }

    @Test
    void rejectsMissingUserRole() {
        assertThrows(
                IllegalArgumentException.class,
                () -> enforcer.evaluate(
                        Severity.MAJOR, null,
                        false, "REASON", false, null, null
                )
        );
    }
}