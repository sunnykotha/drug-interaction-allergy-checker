package com.example.drugchecker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InteractionChecker {

    private final List<InteractionRule> rules;

    public InteractionChecker() {
        this(List.of(
                new InteractionRule(
                        "anticoagulant",
                        "nsaid",
                        Severity.MAJOR,
                        "Increased bleeding risk"
                )
        ));
    }

    public InteractionChecker(List<InteractionRule> rules) {
        if (rules == null) {
            throw new IllegalArgumentException("Interaction rules are required");
        }
        if (rules.stream().anyMatch(rule -> rule == null)) {
            throw new IllegalArgumentException("Interaction rules cannot contain null");
        }

        this.rules = List.copyOf(rules);
    }

    public List<InteractionAlert> check(Prescription prescription) {
        if (prescription == null) {
            throw new IllegalArgumentException("Prescription is required");
        }

        Medication prescribed = prescription.medication();

        if (!prescribed.active()) {
            throw new IllegalArgumentException(
                    "Cannot check a discontinued prescribed medication");
        }

        List<InteractionAlert> alerts = new ArrayList<>();

        for (Medication existing : prescription.patient().activeMedications()) {
            if (!existing.active()) {
                continue;
            }

            if (sameClass(prescribed.drugClass(), existing.drugClass())) {
                alerts.add(new InteractionAlert(
                        prescribed,
                        existing,
                        InteractionAlertType.DUPLICATE_THERAPY,
                        Severity.MODERATE,
                        "Both medications belong to the same drug class"
                ));
                continue;
            }

            for (InteractionRule rule : rules) {
                if (rule.matches(prescribed.drugClass(), existing.drugClass())) {
                    alerts.add(new InteractionAlert(
                            prescribed,
                            existing,
                            InteractionAlertType.DRUG_INTERACTION,
                            rule.severity(),
                            rule.mechanism()
                    ));
                }
            }
        }

        return List.copyOf(alerts);
    }

    private static boolean sameClass(String first, String second) {
        return first.trim().toLowerCase(Locale.ROOT)
                .equals(second.trim().toLowerCase(Locale.ROOT));
    }
}