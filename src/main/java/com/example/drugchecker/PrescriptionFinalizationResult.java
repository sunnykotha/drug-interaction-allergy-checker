package com.example.drugchecker;

import java.util.List;

public record PrescriptionFinalizationResult(
        boolean finalized,
        List<InteractionAlert> interactionAlerts,
        List<AllergyAlert> allergyAlerts,
        List<DoseAlert> doseAlerts,
        List<EnforcementDecision> enforcementDecisions,
        String explanation) {

    public PrescriptionFinalizationResult {
        interactionAlerts = List.copyOf(interactionAlerts);
        allergyAlerts = List.copyOf(allergyAlerts);
        doseAlerts = List.copyOf(doseAlerts);
        enforcementDecisions = List.copyOf(enforcementDecisions);

        if (explanation == null || explanation.isBlank()) {
            throw new IllegalArgumentException("Explanation is required");
        }

        explanation = explanation.trim();
    }
}