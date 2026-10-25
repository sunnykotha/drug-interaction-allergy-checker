package com.example.drugchecker;

public record Medication(
        Long id,
        String brandName,
        String genericIngredient,
        String drugClass,
        boolean active
) {
    public Medication {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Medication ID must be positive");
        }
        if (brandName == null || brandName.isBlank()) {
            throw new IllegalArgumentException("Brand name is required");
        }
        if (genericIngredient == null || genericIngredient.isBlank()) {
            throw new IllegalArgumentException("Generic ingredient is required");
        }
        if (drugClass == null || drugClass.isBlank()) {
            throw new IllegalArgumentException("Drug class is required");
        }

        brandName = brandName.trim();
        genericIngredient = genericIngredient.trim();
        drugClass = drugClass.trim();
    }
}