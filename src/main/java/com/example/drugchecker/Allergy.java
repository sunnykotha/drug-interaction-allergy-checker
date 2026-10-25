package com.example.drugchecker;

import java.util.List;

public record Allergy(
        Long id,
        String allergen,
        String reaction,
        AllergySeverity severity,
        List<String> relatedDrugClasses
) {
    public Allergy {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Allergy ID must be positive");
        }
        if (allergen == null || allergen.isBlank()) {
            throw new IllegalArgumentException("Allergen is required");
        }
        if (reaction == null || reaction.isBlank()) {
            throw new IllegalArgumentException("Reaction is required");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Allergy severity is required");
        }
        if (relatedDrugClasses == null) {
            throw new IllegalArgumentException("Related drug classes are required");
        }
        if (relatedDrugClasses.stream().anyMatch(
                drugClass -> drugClass == null || drugClass.isBlank())) {
            throw new IllegalArgumentException("Related drug classes cannot be blank");
        }

        allergen = allergen.trim();
        reaction = reaction.trim();
        relatedDrugClasses = relatedDrugClasses.stream()
                .map(String::trim)
                .toList();
    }
}