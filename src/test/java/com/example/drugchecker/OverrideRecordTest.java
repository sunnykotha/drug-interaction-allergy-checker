package com.example.drugchecker;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OverrideRecordTest {

    private final LocalDateTime timestamp =
            LocalDateTime.of(2026, 10, 25, 10, 0);

    @Test
    void createsValidMajorOverrideByPhysician() {
        OverrideRecord record = new OverrideRecord(
                1L, 10L, Severity.MAJOR, UserRole.PHYSICIAN,
                "BENEFIT_OUTWEIGHS_RISK", "Clinical benefit outweighs risk",
                false, null, null, timestamp
        );

        assertEquals(UserRole.PHYSICIAN, record.overriddenByRole());
        assertEquals("BENEFIT_OUTWEIGHS_RISK", record.reasonCode());
        assertEquals(10L, record.prescriptionId());
    }

    @Test
    void rejectsMajorOverrideByNurse() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OverrideRecord(
                        1L, 10L, Severity.MAJOR, UserRole.NURSE,
                        "REASON", "Documented reason",
                        false, null, null, timestamp
                )
        );
    }

    @Test
    void rejectsOverrideWithoutReasonCode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OverrideRecord(
                        1L, 10L, Severity.MAJOR, UserRole.PHYSICIAN,
                        " ", "Documented reason",
                        false, null, null, timestamp
                )
        );
    }

    @Test
    void rejectsContraindicatedOverrideWithoutEmergencyDocumentation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OverrideRecord(
                        1L, 10L, Severity.CONTRAINDICATED, UserRole.PHYSICIAN,
                        "EMERGENCY", "Emergency treatment required",
                        false, 99L, UserRole.PHARMACIST, timestamp
                )
        );
    }

    @Test
    void rejectsContraindicatedOverrideWithoutSecondSignOff() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OverrideRecord(
                        1L, 10L, Severity.CONTRAINDICATED, UserRole.PHYSICIAN,
                        "EMERGENCY", "Emergency treatment required",
                        true, null, null, timestamp
                )
        );
    }

    @Test
    void acceptsDocumentedEmergencyWithSecondSignOff() {
        OverrideRecord record = new OverrideRecord(
                1L, 10L, Severity.CONTRAINDICATED, UserRole.PHYSICIAN,
                "EMERGENCY", "Emergency treatment required",
                true, 99L, UserRole.PHARMACIST, timestamp
        );

        assertTrue(record.emergencyDocumented());
        assertEquals(99L, record.secondSignOffUserId());
        assertEquals(UserRole.PHARMACIST, record.secondSignOffRole());
    }

    @Test
    void rejectsIncompleteSecondSignOff() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OverrideRecord(
                        1L, 10L, Severity.MAJOR, UserRole.PHYSICIAN,
                        "REASON", "Documented reason",
                        false, 99L, null, timestamp
                )
        );
    }

    @Test
    void rejectsMissingOverrideTimestamp() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OverrideRecord(
                        1L, 10L, Severity.MODERATE, UserRole.NURSE,
                        "ACKNOWLEDGED", "Alert acknowledged",
                        false, null, null, null
                )
        );
    }
}