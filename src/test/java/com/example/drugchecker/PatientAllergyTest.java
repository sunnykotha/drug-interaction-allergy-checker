package com.example.drugchecker;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatientAllergyTest {

    @Test
    void allergySeverityDistinguishesMildAndLifeThreateningReactions() {
        assertNotEquals(AllergySeverity.MILD, AllergySeverity.LIFE_THREATENING);
    }

    @Test
    void createsAllergyWithRelatedDrugClasses() {
        Allergy allergy = new Allergy(
                1L,
                "Penicillin",
                "Anaphylaxis",
                AllergySeverity.LIFE_THREATENING,
                List.of("Penicillins", "Related beta-lactams")
        );

        assertEquals("Penicillin", allergy.allergen());
        assertEquals("Anaphylaxis", allergy.reaction());
        assertEquals(AllergySeverity.LIFE_THREATENING, allergy.severity());
        assertEquals(2, allergy.relatedDrugClasses().size());
    }

    @Test
    void rejectsAllergyWithoutSeverity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Allergy(1L, "Penicillin", "Rash", null, List.of())
        );
    }

    @Test
    void createsPatientWithClinicalDetails() {
        Patient patient = new Patient(
                1L, 35, 72.5, 90.0, false, List.of(), List.of()
        );

        assertEquals(35, patient.ageYears());
        assertEquals(72.5, patient.weightKg());
        assertEquals(90.0, patient.kidneyFunction());
        assertFalse(patient.pregnant());
        assertTrue(patient.activeMedications().isEmpty());
        assertTrue(patient.allergies().isEmpty());
    }

    @Test
    void rejectsNegativeAge() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Patient(1L, -1, 72.5, 90.0, false, List.of(), List.of())
        );
    }

    @Test
    void rejectsNonPositiveWeight() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Patient(1L, 35, 0, 90.0, false, List.of(), List.of())
        );
    }

    @Test
    void rejectsNegativeKidneyFunction() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Patient(1L, 35, 72.5, -1, false, List.of(), List.of())
        );
    }

    @Test
    void patientCopiesMedicationAndAllergyLists() {
        List<Medication> medications = new ArrayList<>();
        List<Allergy> allergies = new ArrayList<>();

        Patient patient = new Patient(
                1L, 35, 72.5, 90.0, false, medications, allergies
        );

        medications.add(new Medication(
                1L, "Brand A", "Generic A", "Class A", true
        ));

        assertTrue(patient.activeMedications().isEmpty());
        assertThrows(
                UnsupportedOperationException.class,
                () -> patient.allergies().add(
                        new Allergy(
                                1L, "Penicillin", "Rash",
                                AllergySeverity.MILD, List.of()
                        )
                )
        );
    }
}