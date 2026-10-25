package com.example.drugchecker;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class MedicationNameMapper {

    private final Map<String, String> brandToGeneric;

    public MedicationNameMapper(Map<String, String> brandToGeneric) {
        if (brandToGeneric == null) {
            throw new IllegalArgumentException("Brand-to-generic mappings are required");
        }

        Map<String, String> normalizedMappings = new HashMap<>();

        brandToGeneric.forEach((brand, generic) -> {
            if (brand == null || brand.isBlank()) {
                throw new IllegalArgumentException("Brand names cannot be blank");
            }
            if (generic == null || generic.isBlank()) {
                throw new IllegalArgumentException("Generic ingredients cannot be blank");
            }

            String normalizedBrand = normalize(brand);
            String normalizedGeneric = normalize(generic);

            String existing = normalizedMappings.putIfAbsent(
                    normalizedBrand, normalizedGeneric
            );

            if (existing != null && !existing.equals(normalizedGeneric)) {
                throw new IllegalArgumentException(
                        "A brand name cannot map to multiple generic ingredients");
            }
        });

        this.brandToGeneric = Map.copyOf(normalizedMappings);
    }

    public Optional<String> resolveGenericIngredient(String medicationName) {
        if (medicationName == null || medicationName.isBlank()) {
            return Optional.empty();
        }

        String normalizedName = normalize(medicationName);

        String mappedGeneric = brandToGeneric.get(normalizedName);
        if (mappedGeneric != null) {
            return Optional.of(mappedGeneric);
        }

        if (brandToGeneric.containsValue(normalizedName)) {
            return Optional.of(normalizedName);
        }

        return Optional.empty();
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}