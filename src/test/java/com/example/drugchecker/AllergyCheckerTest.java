package com.example.drugchecker;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AllergyCheckerTest {

    private Medication medication(String ingredient, String drugClass) {
        return new Medication(1L, "Test Brand", ingredient, drugClass, true);
    }

    private Allergy allergy(
            String allergen,
            AllergySeverity severity,
            List<String> relatedClasses) {
        return new Allergy(
                1L, allergen, "Recorded reaction", severity, relatedClasses
        );
    }

    private Prescription prescription(
            Medication medication,
            List<Allergy> allergies) {
        Patient patient = new Patient(
                1L, 35, 72.5, 90.0, false, List.of(), allergies
        );

        return new Prescription(
                1L, patient, medication, 250, "mg",
                LocalDateTime.of(2026, 10, 25, 10, 0)
        );
    }

    @Test
    void detectsDirectIngredientAllergy() {
        Medication medication = medication("amoxicillin", "penicillin antibiotic");
        Allergy allergy = allergy(
                "Amoxicillin", AllergySeverity.SEVERE, List.of()
        );

        List<AllergyAlert> alerts = new AllergyChecker()
                .check(prescription(medication, List.of(allergy)));

        assertEquals(1, alerts.size());
        assertEquals(AllergyAlertType.DIRECT_ALLERGY, alerts.get(0).type());
        assertEquals(AllergySeverity.SEVERE, alerts.get(0).allergy().severity());
    }

    @Test
    void detectsCrossReactivityForRecordedRelatedClass() {
        Medication medication = medication("cephalexin", "cephalosporin");

        Allergy allergy = allergy(
                "Penicillin",
                AllergySeverity.MODERATE,
                List.of("Cephalosporin")
        );

        List<AllergyAlert> alerts = new AllergyChecker()
                .check(prescription(medication, List.of(allergy)));

        assertEquals(1, alerts.size());
        assertEquals(
                AllergyAlertType.CROSS_REACTIVITY,
                alerts.get(0).type()
        );
    }

    @Test
    void preservesLifeThreateningAllergySeverity() {
        Medication medication = medication("penicillin", "penicillin antibiotic");

        Allergy allergy = allergy(
                "penicillin",
                AllergySeverity.LIFE_THREATENING,
                List.of()
        );

        List<AllergyAlert> alerts = new AllergyChecker()
                .check(prescription(medication, List.of(allergy)));

        assertEquals(1, alerts.size());
        assertEquals(
                AllergySeverity.LIFE_THREATENING,
                alerts.get(0).allergy().severity()
        );
    }

    @Test
    void doesNotAlertForUnrelatedAllergy() {
        Medication medication = medication("ibuprofen", "NSAID");

        Allergy allergy = allergy(
                "penicillin",
                AllergySeverity.MILD,
                List.of("Penicillin antibiotic")
        );

        List<AllergyAlert> alerts = new AllergyChecker()
                .check(prescription(medication, List.of(allergy)));

        assertTrue(alerts.isEmpty());
    }

    @Test
    void checksAllRecordedAllergies() {
        Medication medication = medication("amoxicillin", "penicillin antibiotic");

        Allergy unrelated = allergy(
                "latex", AllergySeverity.MILD, List.of()
        );
        Allergy matching = allergy(
                "amoxicillin", AllergySeverity.SEVERE, List.of()
        );

        List<AllergyAlert> alerts = new AllergyChecker()
                .check(prescription(medication, List.of(unrelated, matching)));

        assertEquals(1, alerts.size());
        assertEquals("amoxicillin", alerts.get(0).allergy().allergen());
    }

    @Test
    void doesNotProduceDuplicateAlertWhenDirectMatchAlsoHasRelatedClass() {
        Medication medication = medication("amoxicillin", "penicillin antibiotic");

        Allergy allergy = allergy(
                "amoxicillin",
                AllergySeverity.SEVERE,
                List.of("penicillin antibiotic")
        );

        List<AllergyAlert> alerts = new AllergyChecker()
                .check(prescription(medication, List.of(allergy)));

        assertEquals(1, alerts.size());
        assertEquals(AllergyAlertType.DIRECT_ALLERGY, alerts.get(0).type());
    }

    @Test
    void rejectsNullPrescription() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AllergyChecker().check(null)
        );
    }

    @Test
    void rejectsDiscontinuedPrescribedMedication() {
        Medication medication = new Medication(
                1L, "Test Brand", "ingredient", "Test class", false
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new AllergyChecker().check(prescription(medication, List.of()))
        );
    }
}