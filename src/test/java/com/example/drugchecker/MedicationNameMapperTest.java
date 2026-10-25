package com.example.drugchecker;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MedicationNameMapperTest {

    private MedicationNameMapper mapper() {
        return new MedicationNameMapper(Map.of(
                "Brand A", "generic-a",
                "Brand B", "generic-b"
        ));
    }

    @Test
    void mapsBrandNameToGenericIngredient() {
        assertEquals(
                Optional.of("generic-a"),
                mapper().resolveGenericIngredient("Brand A")
        );
    }

    @Test
    void mapsBrandNameIgnoringCaseAndWhitespace() {
        assertEquals(
                Optional.of("generic-a"),
                mapper().resolveGenericIngredient("  BRAND A  ")
        );
    }

    @Test
    void recognizesKnownGenericIngredient() {
        assertEquals(
                Optional.of("generic-b"),
                mapper().resolveGenericIngredient(" GENERIC-B ")
        );
    }

    @Test
    void returnsEmptyForUnknownMedicationName() {
        assertEquals(
                Optional.empty(),
                mapper().resolveGenericIngredient("Unknown Brand")
        );
    }

    @Test
    void returnsEmptyForBlankOrNullName() {
        MedicationNameMapper mapper = mapper();

        assertEquals(Optional.empty(), mapper.resolveGenericIngredient(" "));
        assertEquals(Optional.empty(), mapper.resolveGenericIngredient(null));
    }

    @Test
    void rejectsNullMappingCollection() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MedicationNameMapper(null)
        );
    }

    @Test
    void rejectsBlankBrandName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MedicationNameMapper(Map.of(" ", "generic-a"))
        );
    }

    @Test
    void rejectsBlankGenericIngredient() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MedicationNameMapper(Map.of("Brand A", " "))
        );
    }
}