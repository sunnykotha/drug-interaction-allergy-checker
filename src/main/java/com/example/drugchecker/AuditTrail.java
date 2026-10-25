package com.example.drugchecker;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditTrail {

    private final List<AuditEntry> entries = new ArrayList<>();
    private long nextId = 1;

    public synchronized AuditEntry recordInteractionAlert(
            Long prescriptionId,
            InteractionAlert alert) {
        if (alert == null) {
            throw new IllegalArgumentException("Interaction alert is required");
        }

        return add(
                prescriptionId,
                AuditEventType.INTERACTION_ALERT,
                alert.severity(),
                null,
                null,
                alert.type() + ": " + alert.mechanism(),
                null
        );
    }

    public synchronized AuditEntry recordAllergyAlert(
            Long prescriptionId,
            AllergyAlert alert) {
        if (alert == null) {
            throw new IllegalArgumentException("Allergy alert is required");
        }

        return add(
                prescriptionId,
                AuditEventType.ALLERGY_ALERT,
                null,
                alert.allergy().severity(),
                null,
                alert.type() + ": " + alert.explanation()
                        + "; allergen: " + alert.allergy().allergen()
                        + "; reaction: " + alert.allergy().reaction(),
                null
        );
    }

    public synchronized AuditEntry recordDoseAlert(
            Long prescriptionId,
            DoseAlert alert) {
        if (alert == null) {
            throw new IllegalArgumentException("Dose alert is required");
        }

        return add(
                prescriptionId,
                AuditEventType.DOSE_ALERT,
                null,
                null,
                null,
                String.join("; ", alert.issues()),
                null
        );
    }

    public synchronized AuditEntry recordDecision(
            Long prescriptionId,
            EnforcementDecision decision,
            UserRole actorRole,
            String reasonCode) {
        if (decision == null) {
            throw new IllegalArgumentException("Enforcement decision is required");
        }
        if (actorRole == null) {
            throw new IllegalArgumentException("Actor role is required");
        }

        return add(
                prescriptionId,
                AuditEventType.ENFORCEMENT_DECISION,
                null,
                null,
                actorRole,
                decision.action() + ": " + decision.explanation(),
                reasonCode
        );
    }

    public synchronized AuditEntry recordOverride(OverrideRecord overrideRecord) {
        if (overrideRecord == null) {
            throw new IllegalArgumentException("Override record is required");
        }

        String details = "Override recorded; reason: "
                + overrideRecord.reason()
                + "; emergency documented: "
                + overrideRecord.emergencyDocumented();

        if (overrideRecord.secondSignOffUserId() != null) {
            details += "; second sign-off user ID: "
                    + overrideRecord.secondSignOffUserId()
                    + "; second sign-off role: "
                    + overrideRecord.secondSignOffRole();
        }

        return add(
                overrideRecord.prescriptionId(),
                AuditEventType.OVERRIDE_RECORDED,
                overrideRecord.severity(),
                null,
                overrideRecord.overriddenByRole(),
                details,
                overrideRecord.reasonCode()
        );
    }

    public synchronized List<AuditEntry> entriesForPrescription(
            Long prescriptionId) {
        if (prescriptionId == null || prescriptionId <= 0) {
            throw new IllegalArgumentException("Prescription ID must be positive");
        }

        return entries.stream()
                .filter(entry -> entry.prescriptionId().equals(prescriptionId))
                .toList();
    }

    public synchronized List<AuditEntry> entriesForPharmacistReview(
            Long prescriptionId,
            UserRole reviewerRole) {
        if (reviewerRole == null) {
            throw new IllegalArgumentException("Reviewer role is required");
        }
        if (reviewerRole != UserRole.PHARMACIST) {
            throw new SecurityException(
                    "Only pharmacists may review prescription audit entries");
        }

        return entriesForPrescription(prescriptionId);
    }

    public synchronized List<AuditEntry> allEntries() {
        return List.copyOf(entries);
    }

    private AuditEntry add(
            Long prescriptionId,
            AuditEventType eventType,
            Severity severity,
            AllergySeverity allergySeverity,
            UserRole actorRole,
            String details,
            String reasonCode) {

        AuditEntry entry = new AuditEntry(
                nextId++,
                prescriptionId,
                eventType,
                severity,
                allergySeverity,
                actorRole,
                details,
                reasonCode,
                LocalDateTime.now()
        );

        entries.add(entry);
        return entry;
    }
}