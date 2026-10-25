package com.example.drugchecker;

public record EnforcementDecision(
        EnforcementAction action,
        boolean allowed,
        String explanation
) {
    public EnforcementDecision {
        if (action == null) {
            throw new IllegalArgumentException("Enforcement action is required");
        }
        if (explanation == null || explanation.isBlank()) {
            throw new IllegalArgumentException("Explanation is required");
        }
        if (allowed != (action == EnforcementAction.ALLOW
                || action == EnforcementAction.ALLOW_WITH_OVERRIDE)) {
            throw new IllegalArgumentException(
                    "Allowed status must match the enforcement action");
        }

        explanation = explanation.trim();
    }
}