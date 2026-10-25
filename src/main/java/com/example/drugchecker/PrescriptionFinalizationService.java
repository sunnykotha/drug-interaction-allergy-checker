package com.example.drugchecker;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class PrescriptionFinalizationService {

    private static final AtomicLong NEXT_OVERRIDE_ID =
            new AtomicLong(1_000_000L);

    private final InteractionChecker interactionChecker;
    private final AllergyChecker allergyChecker;
    private final DoseChecker doseChecker;
    private final SeverityEnforcer severityEnforcer;
    private final AllergyEnforcer allergyEnforcer;
    private final AuditTrail auditTrail;
    private final MedicationStatusGuard medicationStatusGuard;
    private final MedicationStatusProvider medicationStatusProvider;

    public PrescriptionFinalizationService(
            InteractionChecker interactionChecker,
            AllergyChecker allergyChecker,
            DoseChecker doseChecker,
            SeverityEnforcer severityEnforcer,
            AuditTrail auditTrail,
            MedicationStatusGuard medicationStatusGuard,
            MedicationStatusProvider medicationStatusProvider) {

        if (interactionChecker == null
                || allergyChecker == null
                || doseChecker == null
                || severityEnforcer == null
                || auditTrail == null
                || medicationStatusGuard == null
                || medicationStatusProvider == null) {
            throw new IllegalArgumentException(
                    "All finalization dependencies are required");
        }

        this.interactionChecker = interactionChecker;
        this.allergyChecker = allergyChecker;
        this.doseChecker = doseChecker;
        this.severityEnforcer = severityEnforcer;
        this.allergyEnforcer = new AllergyEnforcer();
        this.auditTrail = auditTrail;
        this.medicationStatusGuard = medicationStatusGuard;
        this.medicationStatusProvider = medicationStatusProvider;
    }

    public PrescriptionFinalizationResult finalizePrescription(
            Prescription prescription,
            UserRole actorRole,
            boolean acknowledged,
            String reasonCode,
            boolean emergencyDocumented,
            Long secondSignOffUserId,
            UserRole secondSignOffRole) {

        return finalizePrescriptionInternal(
                prescription, null, actorRole, acknowledged, reasonCode,
                emergencyDocumented, secondSignOffUserId, secondSignOffRole);
    }

    public PrescriptionFinalizationResult finalizePrescription(
            Prescription prescription,
            Long actorUserId,
            UserRole actorRole,
            boolean acknowledged,
            String reasonCode,
            boolean emergencyDocumented,
            Long secondSignOffUserId,
            UserRole secondSignOffRole) {

        if (actorUserId == null || actorUserId <= 0) {
            throw new IllegalArgumentException(
                    "Primary authorizer user ID must be positive");
        }

        return finalizePrescriptionInternal(
                prescription, actorUserId, actorRole, acknowledged, reasonCode,
                emergencyDocumented, secondSignOffUserId, secondSignOffRole);
    }

    private PrescriptionFinalizationResult finalizePrescriptionInternal(
            Prescription prescription,
            Long actorUserId,
            UserRole actorRole,
            boolean acknowledged,
            String reasonCode,
            boolean emergencyDocumented,
            Long secondSignOffUserId,
            UserRole secondSignOffRole) {

        if (prescription == null) {
            throw new IllegalArgumentException("Prescription is required");
        }
        if (actorRole == null) {
            throw new IllegalArgumentException("Actor role is required");
        }

        medicationStatusGuard.ensureActive(
                prescription.medication(), medicationStatusProvider);

        List<InteractionAlert> interactionAlerts =
                interactionChecker.check(prescription);
        List<AllergyAlert> allergyAlerts =
                allergyChecker.check(prescription);
        List<DoseAlert> doseAlerts =
                doseChecker.check(prescription);

        for (InteractionAlert alert : interactionAlerts) {
            auditTrail.recordInteractionAlert(prescription.id(), alert);
        }

        for (AllergyAlert alert : allergyAlerts) {
            auditTrail.recordAllergyAlert(prescription.id(), alert);
        }

        for (DoseAlert alert : doseAlerts) {
            auditTrail.recordDoseAlert(prescription.id(), alert);
        }

        List<EnforcementDecision> decisions = new ArrayList<>();

        for (AllergyAlert alert : allergyAlerts) {
            EnforcementDecision decision = allergyEnforcer.evaluate(
                    alert, acknowledged);

            decisions.add(decision);
            auditTrail.recordDecision(
                    prescription.id(), decision, actorRole, reasonCode);
        }

        for (InteractionAlert alert : interactionAlerts) {
            EnforcementDecision decision = severityEnforcer.evaluate(
                    alert.severity(),
                    actorRole,
                    acknowledged,
                    reasonCode,
                    emergencyDocumented,
                    secondSignOffUserId,
                    secondSignOffRole,
                    actorUserId
            );

            decisions.add(decision);
            auditTrail.recordDecision(
                    prescription.id(), decision, actorRole, reasonCode);

            if (decision.action() == EnforcementAction.ALLOW_WITH_OVERRIDE) {
                OverrideRecord overrideRecord = new OverrideRecord(
                        NEXT_OVERRIDE_ID.getAndIncrement(),
                        prescription.id(),
                        alert.severity(),
                        actorRole,
                        reasonCode,
                        decision.explanation(),
                        emergencyDocumented,
                        secondSignOffUserId,
                        secondSignOffRole,
                        LocalDateTime.now()
                );

                auditTrail.recordOverride(overrideRecord);
            }
        }

        boolean enforcementAllowed = decisions.stream()
                .allMatch(EnforcementDecision::allowed);

        boolean checksPassed = doseAlerts.isEmpty() && enforcementAllowed;

        medicationStatusGuard.ensureActive(
                prescription.medication(), medicationStatusProvider);

        boolean finalized = checksPassed;

        String explanation = finalized
                ? "Prescription checks passed."
                : "Prescription not finalized: one or more safety checks "
                        + "or enforcement decisions require review.";

        return new PrescriptionFinalizationResult(
                finalized,
                interactionAlerts,
                allergyAlerts,
                doseAlerts,
                decisions,
                explanation
        );
    }
}