package com.example.drugchecker;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BrandMappingIntegrationTest {

    @Test
    void resolvesBrandNameBeforeCheckingDirectAllergy() {
        Medication prescribed = new Medication(
                2L, "Brand-X", "incorrect-ingredient", "class-a", true);

        Allergy allergy = new Allergy(
                1L, "penicillin", "rash",
                AllergySeverity.MILD, List.of());

        Patient patient = new Patient(
                10L, 35, 65.0, 90.0, false,
                List.of(), List.of(allergy));

        Prescription prescription = new Prescription(
                100L, patient, prescribed, 10.0, "mg",
                LocalDateTime.now());

        MedicationNameMapper mapper = new MedicationNameMapper(
                Map.of("Brand-X", "penicillin"));

        List<AllergyAlert> alerts = new AllergyChecker(mapper).check(prescription);

        assertEquals(1, alerts.size());
        assertEquals(AllergyAlertType.DIRECT_ALLERGY, alerts.get(0).type());
    }

    @Test
    void retainsMedicationIngredientWhenBrandIsUnknown() {
        Medication prescribed = new Medication(
                2L, "Unknown-Brand", "penicillin", "class-a", true);

        Allergy allergy = new Allergy(
                1L, "penicillin", "rash",
                AllergySeverity.MILD, List.of());

        Patient patient = new Patient(
                10L, 35, 65.0, 90.0, false,
                List.of(), List.of(allergy));

        Prescription prescription = new Prescription(
                101L, patient, prescribed, 10.0, "mg",
                LocalDateTime.now());

        List<AllergyAlert> alerts = new AllergyChecker().check(prescription);

        assertEquals(1, alerts.size());
        assertEquals(AllergyAlertType.DIRECT_ALLERGY, alerts.get(0).type());
    }
}