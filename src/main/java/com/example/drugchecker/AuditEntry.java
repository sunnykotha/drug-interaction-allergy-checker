package com.example.drugchecker;

import java.time.LocalDateTime;

public record AuditEntry(
        Long id,
        Long prescriptionId,
        AuditEventType eventType,
        Severity severity,
        AllergySeverity allergySeverity,
        UserRole actorRole,
        String details,
        String reasonCode,
        LocalDateTime occurredAt
) {
    public AuditEntry {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Audit ID must be positive");
        }
        if (prescriptionId == null || prescriptionId <= 0) {
            throw new IllegalArgumentException("Prescription ID must be positive");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("Audit event type is required");
        }
        if (details == null || details.isBlank()) {
            throw new IllegalArgumentException("Audit details are required");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("Audit timestamp is required");
        }

        details = details.trim();

        if (reasonCode != null) {
            reasonCode = reasonCode.trim();
            if (reasonCode.isEmpty()) {
                reasonCode = null;
            }
        }
    }
}