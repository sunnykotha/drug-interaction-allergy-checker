package com.example.drugchecker;

import java.time.LocalDateTime;

public record OverrideRecord(
        Long id,
        Long prescriptionId,
        Severity severity,
        UserRole overriddenByRole,
        String reasonCode,
        String reason,
        boolean emergencyDocumented,
        Long secondSignOffUserId,
        UserRole secondSignOffRole,
        LocalDateTime createdAt
) {
    public OverrideRecord {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Override ID must be positive");
        }
        if (prescriptionId == null || prescriptionId <= 0) {
            throw new IllegalArgumentException("Prescription ID must be positive");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Severity is required");
        }
        if (overriddenByRole == null) {
            throw new IllegalArgumentException("Authorizing user role is required");
        }
        if (reasonCode == null || reasonCode.isBlank()) {
            throw new IllegalArgumentException("Reason code is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Override reason is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Override timestamp is required");
        }

        reasonCode = reasonCode.trim();
        reason = reason.trim();

        boolean hasSecondSignOff =
                secondSignOffUserId != null || secondSignOffRole != null;

        if (hasSecondSignOff
                && (secondSignOffUserId == null
                || secondSignOffUserId <= 0
                || secondSignOffRole == null)) {
            throw new IllegalArgumentException(
                    "Second sign-off requires a valid user ID and role");
        }

        if (secondSignOffUserId != null
                && secondSignOffUserId.equals(id)) {
            throw new IllegalArgumentException(
                    "Override ID cannot be used as a sign-off user ID");
        }

        if (severity == Severity.CONTRAINDICATED
                && (!emergencyDocumented || !hasSecondSignOff)) {
            throw new IllegalArgumentException(
                    "Contraindicated override requires documented emergency "
                            + "and second sign-off");
        }

        if (severity == Severity.MAJOR
                && overriddenByRole != UserRole.PHYSICIAN) {
            throw new IllegalArgumentException(
                    "Major interaction override requires a physician");
        }

        if (severity == Severity.MINOR || severity == Severity.MODERATE) {
            if (overriddenByRole == UserRole.NURSE
                    || overriddenByRole == UserRole.PHYSICIAN
                    || overriddenByRole == UserRole.PHARMACIST) {
                // Role is recorded; acknowledgment is handled by the
                // enforcement service.
            }
        }
    }
}