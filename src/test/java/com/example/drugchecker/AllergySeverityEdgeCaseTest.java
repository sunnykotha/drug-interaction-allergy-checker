package com.example.drugchecker;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AllergySeverityEdgeCaseTest {

    private Medication medication() {
        return new Medication(
                10L, "Brand A", "generic-a", "class-a", true
        );
    }

    private AllergyAlert checkAllergy(
            AllergySeverity severity, String reaction) {

        Allergy allergy = new Allergy(
                1L,
                "generic-a",
                reaction,
                severity,
                List.of()
        );

        Patient patient = new Patient(
                1L,
                35,
                70.0,
                90.0,
                false,
                List.of(),
                List.of(allergy)
        );

        Prescription prescription = new Prescription(
                100L,
                patient,
                medication(),
                10.0,
                "mg",
                LocalDateTime.now()
        );

        return new AllergyChecker()
                .check(prescription)
                .get(0);
    }

    @Test
    void preservesMildRashSeverity() {
        AllergyAlert alert = checkAllergy(
                AllergySeverity.MILD, "rash"
        );

        assertEquals(AllergySeverity.MILD, alert.allergy().severity());
        assertEquals("rash", alert.allergy().reaction());
        assertEquals(AllergyAlertType.DIRECT_ALLERGY, alert.type());
    }

    @Test
    void preservesLifeThreateningReactionSeverity() {
        AllergyAlert alert = checkAllergy(
                AllergySeverity.LIFE_THREATENING, "anaphylaxis"
        );

        assertEquals(
                AllergySeverity.LIFE_THREATENING,
                alert.allergy().severity()
        );
        assertEquals("anaphylaxis", alert.allergy().reaction());
    }

    @Test
    void distinguishesMildRashFromAnaphylaxis() {
        AllergyAlert rash = checkAllergy(
                AllergySeverity.MILD, "rash"
        );
        AllergyAlert anaphylaxis = checkAllergy(
                AllergySeverity.LIFE_THREATENING, "anaphylaxis"
        );

        assertNotEquals(
                rash.allergy().severity(),
                anaphylaxis.allergy().severity()
        );
        assertNotEquals(
                rash.allergy().reaction(),
                anaphylaxis.allergy().reaction()
        );
    }

    @Test
    void preservesSevereReactionSeverity() {
        AllergyAlert alert = checkAllergy(
                AllergySeverity.SEVERE, "swelling"
        );

        assertEquals(AllergySeverity.SEVERE, alert.allergy().severity());
        assertEquals("swelling", alert.allergy().reaction());
    }

    @Test
    void doesNotLoseAllergySeverityInAlert() {
        AllergyAlert alert = checkAllergy(
                AllergySeverity.LIFE_THREATENING, "anaphylaxis"
        );

        assertNotNull(alert.allergy());
        assertNotNull(alert.explanation());
        assertTrue(!alert.explanation().isBlank());
    }

    @Test
    void stillReportsDirectAllergyForDifferentReactionSeverities() {
        AllergyAlert mild = checkAllergy(
                AllergySeverity.MILD, "rash"
        );
        AllergyAlert lifeThreatening = checkAllergy(
                AllergySeverity.LIFE_THREATENING, "anaphylaxis"
        );

        assertEquals(AllergyAlertType.DIRECT_ALLERGY, mild.type());
        assertEquals(
                AllergyAlertType.DIRECT_ALLERGY,
                lifeThreatening.type()
        );
    }
}