package com.example.drugchecker;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AllergyEnforcerTest {

    private AllergyAlert alert(
            AllergySeverity severity,
            AllergyAlertType type) {

        Medication medication = new Medication(
                1L, "Example Brand", "example-drug", "example-class", true
        );

        Allergy allergy = new Allergy(
                1L,
                "penicillin",
                "rash",
                severity,
                List.of("beta-lactam")
        );

        return new AllergyAlert(
                medication,
                allergy,
                type,
                "Potential allergy risk"
        );
    }

    private final AllergyEnforcer enforcer = new AllergyEnforcer();

    @Test
    void directAllergyMatchIsBlockedEvenForMildReaction() {
        EnforcementDecision decision = enforcer.evaluate(
                alert(AllergySeverity.MILD, AllergyAlertType.DIRECT_ALLERGY),
                true
        );

        assertEquals(EnforcementAction.BLOCK, decision.action());
        assertFalse(decision.allowed());
    }

    @Test
    void crossReactivityRequiresAcknowledgment() {
        EnforcementDecision decision = enforcer.evaluate(
                alert(AllergySeverity.MILD, AllergyAlertType.CROSS_REACTIVITY),
                false
        );

        assertEquals(
                EnforcementAction.REQUIRE_ACKNOWLEDGMENT,
                decision.action()
        );
        assertFalse(decision.allowed());
    }

    @Test
    void acknowledgedMildCrossReactivityWarningAllowsContinuation() {
        EnforcementDecision decision = enforcer.evaluate(
                alert(AllergySeverity.MILD, AllergyAlertType.CROSS_REACTIVITY),
                true
        );

        assertEquals(EnforcementAction.ALLOW, decision.action());
        assertTrue(decision.allowed());
    }

    @Test
    void severeCrossReactivityIsBlockedEvenWhenAcknowledged() {
        EnforcementDecision decision = enforcer.evaluate(
                alert(AllergySeverity.SEVERE, AllergyAlertType.CROSS_REACTIVITY),
                true
        );

        assertEquals(EnforcementAction.BLOCK, decision.action());
        assertFalse(decision.allowed());
    }

    @Test
    void lifeThreateningCrossReactivityIsBlocked() {
        EnforcementDecision decision = enforcer.evaluate(
                alert(
                        AllergySeverity.LIFE_THREATENING,
                        AllergyAlertType.CROSS_REACTIVITY
                ),
                true
        );

        assertEquals(EnforcementAction.BLOCK, decision.action());
        assertFalse(decision.allowed());
    }

    @Test
    void nullAlertIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> enforcer.evaluate(null, false)
        );
    }
}