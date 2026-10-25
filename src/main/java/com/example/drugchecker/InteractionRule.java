package com.example.drugchecker;

import java.util.Locale;

public record InteractionRule(
        String drugClassA,
        String drugClassB,
        Severity severity,
        String mechanism
) {
    public InteractionRule {
        if (drugClassA == null || drugClassA.isBlank()) {
            throw new IllegalArgumentException("First drug class is required");
        }
        if (drugClassB == null || drugClassB.isBlank()) {
            throw new IllegalArgumentException("Second drug class is required");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Interaction severity is required");
        }
        if (mechanism == null || mechanism.isBlank()) {
            throw new IllegalArgumentException("Interaction mechanism is required");
        }

        drugClassA = normalize(drugClassA);
        drugClassB = normalize(drugClassB);
        mechanism = mechanism.trim();

        if (drugClassA.equals(drugClassB)) {
            throw new IllegalArgumentException(
                    "Interaction rule must contain two different drug classes");
        }

        if (drugClassA.compareTo(drugClassB) > 0) {
            String temporary = drugClassA;
            drugClassA = drugClassB;
            drugClassB = temporary;
        }
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public boolean matches(String classA, String classB) {
        if (classA == null || classB == null
                || classA.isBlank() || classB.isBlank()) {
            return false;
        }

        String normalizedA = normalize(classA);
        String normalizedB = normalize(classB);

        return (drugClassA.equals(normalizedA)
                && drugClassB.equals(normalizedB))
                || (drugClassA.equals(normalizedB)
                && drugClassB.equals(normalizedA));
    }
}