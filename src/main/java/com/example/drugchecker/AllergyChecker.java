package com.example.drugchecker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AllergyChecker {

    private final MedicationNameMapper medicationNameMapper;

    public AllergyChecker() {
        this(new MedicationNameMapper(Map.of()));
    }

    public AllergyChecker(MedicationNameMapper medicationNameMapper) {
        if (medicationNameMapper == null) {
            throw new IllegalArgumentException("Medication name mapper is required");
        }
        this.medicationNameMapper = medicationNameMapper;
    }

    public List<AllergyAlert> check(Prescription prescription) {
        if (prescription == null) {
            throw new IllegalArgumentException("Prescription is required");
        }

        Medication medication = prescription.medication();

        if (!medication.active()) {
            throw new IllegalArgumentException(
                    "Cannot check a discontinued prescribed medication");
        }

        String genericIngredient = medicationNameMapper
                .resolveGenericIngredient(medication.brandName())
                .orElse(medication.genericIngredient());

        List<AllergyAlert> alerts = new ArrayList<>();

        for (Allergy allergy : prescription.patient().allergies()) {
            if (sameValue(genericIngredient, allergy.allergen())) {
                alerts.add(new AllergyAlert(
                        medication,
                        allergy,
                        AllergyAlertType.DIRECT_ALLERGY,
                        "Prescribed ingredient matches the recorded allergen"
                ));
            } else if (allergy.relatedDrugClasses().stream()
                    .anyMatch(drugClass ->
                            sameValue(drugClass, medication.drugClass()))) {
                alerts.add(new AllergyAlert(
                        medication,
                        allergy,
                        AllergyAlertType.CROSS_REACTIVITY,
                        "Prescribed drug class matches a class related "
                                + "to the recorded allergy"
                ));
            }
        }

        return List.copyOf(alerts);
    }

    private static boolean sameValue(String first, String second) {
        return first.trim().toLowerCase(Locale.ROOT)
                .equals(second.trim().toLowerCase(Locale.ROOT));
    }
}