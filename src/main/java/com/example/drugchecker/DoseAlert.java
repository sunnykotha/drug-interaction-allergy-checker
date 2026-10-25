package com.example.drugchecker;

import java.util.List;

public record DoseAlert(
        Prescription prescription,
        List<String> issues
) {
    public DoseAlert {
        if (prescription == null) {
            throw new IllegalArgumentException("Prescription is required");
        }
        if (issues == null || issues.isEmpty()) {
            throw new IllegalArgumentException("At least one dose issue is required");
        }
        if (issues.stream().anyMatch(issue -> issue == null || issue.isBlank())) {
            throw new IllegalArgumentException("Dose issues cannot be blank");
        }

        issues = List.copyOf(issues);
    }
}