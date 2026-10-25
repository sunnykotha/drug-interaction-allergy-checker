package com.example.drugchecker;

public class AllergyEnforcer {

    public EnforcementDecision evaluate(
            AllergyAlert alert,
            boolean acknowledged) {

        if (alert == null) {
            throw new IllegalArgumentException("Allergy alert is required");
        }

        AllergySeverity severity = alert.allergy().severity();

        if (alert.type() == AllergyAlertType.DIRECT_ALLERGY) {
            return new EnforcementDecision(
                    EnforcementAction.BLOCK,
                    false,
                    "Direct allergy match blocks prescription finalization."
            );
        }

        if (severity == AllergySeverity.SEVERE
                || severity == AllergySeverity.LIFE_THREATENING) {
            return new EnforcementDecision(
                    EnforcementAction.BLOCK,
                    false,
                    "Severe or life-threatening allergy reaction blocks "
                            + "prescription finalization."
            );
        }

        if (alert.type() == AllergyAlertType.CROSS_REACTIVITY) {
            if (!acknowledged) {
                return new EnforcementDecision(
                        EnforcementAction.REQUIRE_ACKNOWLEDGMENT,
                        false,
                        "Cross-reactivity warning requires acknowledgment."
                );
            }

            return new EnforcementDecision(
                    EnforcementAction.ALLOW,
                    true,
                    "Cross-reactivity warning acknowledged."
            );
        }

        return new EnforcementDecision(
                EnforcementAction.BLOCK,
                false,
                "Unrecognized allergy alert requires review."
        );
    }
}