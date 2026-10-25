package com.example.drugchecker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MedicationStatusGuardTest {

    private final MedicationStatusGuard guard = new MedicationStatusGuard();

    private Medication medication() {
        return new Medication(1L, "Brand A", "generic-a", "class-a", true);
    }

    @Test
    void allowsMedicationThatIsCurrentlyActive() {
        assertDoesNotThrow(() ->
                guard.ensureActive(medication(), id -> true));
    }

    @Test
    void rejectsMedicationDiscontinuedDuringCheck() {
        Medication medication = medication();

        // The stored object says active, but the live status says discontinued.
        assertTrue(medication.active());

        assertThrows(
                IllegalStateException.class,
                () -> guard.ensureActive(medication, id -> false)
        );
    }

    @Test
    void queriesStatusUsingMedicationId() {
        assertDoesNotThrow(() ->
                guard.ensureActive(medication(), id -> {
                    assertEquals(1L, id);
                    return true;
                }));
    }

    @Test
    void rejectsNullMedication() {
        assertThrows(
                IllegalArgumentException.class,
                () -> guard.ensureActive(null, id -> true)
        );
    }

    @Test
    void rejectsNullStatusProvider() {
        assertThrows(
                IllegalArgumentException.class,
                () -> guard.ensureActive(medication(), null)
        );
    }

    @Test
    void rejectsMedicationWhenLiveStatusIsInactive() {
        assertThrows(
                IllegalStateException.class,
                () -> guard.ensureActive(medication(), id -> false)
        );
    }
}