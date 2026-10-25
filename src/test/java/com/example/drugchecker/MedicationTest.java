package com.example.drugchecker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MedicationTest {

    @Test
    void severityContainsAllRequiredLevels() {
        assertArrayEquals(
                new Severity[]{
                        Severity.MINOR,
                        Severity.MODERATE,
                        Severity.MAJOR,
                        Severity.CONTRAINDICATED
                },
                Severity.values()
        );
    }

    @Test
    void createsMedicationWithValidDetails() {
        Medication medication = new Medication(
                1L, "Brand A", "Generic A", "NSAID", true
        );

        assertEquals("Brand A", medication.brandName());
        assertEquals("Generic A", medication.genericIngredient());
        assertEquals("NSAID", medication.drugClass());
        assertTrue(medication.active());
    }

    @Test
    void trimsMedicationTextFields() {
        Medication medication = new Medication(
                1L, " Brand A ", " Generic A ", " NSAID ", true
        );

        assertEquals("Brand A", medication.brandName());
        assertEquals("Generic A", medication.genericIngredient());
        assertEquals("NSAID", medication.drugClass());
    }

    @Test
    void rejectsInvalidMedicationId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Medication(0L, "Brand A", "Generic A", "NSAID", true)
        );
    }

    @Test
    void rejectsBlankGenericIngredient() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Medication(1L, "Brand A", " ", "NSAID", true)
        );
    }

    @Test
    void rejectsMissingDrugClass() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Medication(1L, "Brand A", "Generic A", null, true)
        );
    }
}