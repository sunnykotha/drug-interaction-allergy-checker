package com.example.drugchecker;

public class SeverityEnforcer {

    public EnforcementDecision evaluate(
            Severity severity,
            UserRole userRole,
            boolean acknowledged,
            String reasonCode,
            boolean emergencyDocumented,
            Long secondSignOffUserId,
            UserRole secondSignOffRole) {

        return evaluate(
                severity, userRole, acknowledged, reasonCode,
                emergencyDocumented, secondSignOffUserId,
                secondSignOffRole, null);
    }

    public EnforcementDecision evaluate(
            Severity severity,
            UserRole userRole,
            boolean acknowledged,
            String reasonCode,
            boolean emergencyDocumented,
            Long secondSignOffUserId,
            UserRole secondSignOffRole,
            Long actorUserId) {

        if (severity == null) {
            throw new IllegalArgumentException("Severity is required");
        }
        if (userRole == null) {
            throw new IllegalArgumentException("User role is required");
        }

        switch (severity) {
            case MINOR, MODERATE -> {
                if (!acknowledged) {
                    return new EnforcementDecision(
                            EnforcementAction.REQUIRE_ACKNOWLEDGMENT,
                            false,
                            "The alert must be acknowledged before proceeding"
                    );
                }

                return new EnforcementDecision(
                        EnforcementAction.ALLOW,
                        true,
                        "The alert has been acknowledged"
                );
            }

            case MAJOR -> {
                if (userRole != UserRole.PHYSICIAN) {
                    return new EnforcementDecision(
                            EnforcementAction.BLOCK,
                            false,
                            "A major interaction requires physician authorization"
                    );
                }

                if (reasonCode == null || reasonCode.isBlank()) {
                    return new EnforcementDecision(
                            EnforcementAction.REQUIRE_REASON_CODE,
                            false,
                            "A reason code is required for a major interaction override"
                    );
                }

                return new EnforcementDecision(
                        EnforcementAction.ALLOW_WITH_OVERRIDE,
                        true,
                        "Major interaction override authorized with a reason code"
                );
            }

            case CONTRAINDICATED -> {
                if (!emergencyDocumented) {
                    return new EnforcementDecision(
                            EnforcementAction.BLOCK,
                            false,
                            "Contraindicated prescriptions are blocked unless an emergency is documented"
                    );
                }

                if (secondSignOffUserId == null
                        || secondSignOffUserId <= 0
                        || secondSignOffRole == null) {
                    return new EnforcementDecision(
                            EnforcementAction.BLOCK,
                            false,
                            "A second authorized sign-off is required"
                    );
                }


                if (reasonCode == null || reasonCode.isBlank()) {
                    return new EnforcementDecision(
                            EnforcementAction.REQUIRE_REASON_CODE,
                            false,
                            "A reason code is required for a contraindicated emergency override"
                    );
                }
                if (actorUserId == null) {
                    return new EnforcementDecision(
                            EnforcementAction.BLOCK,
                            false,
                            "The primary authorizer user ID is required for a contraindicated override"
                    );
                }

                if (actorUserId.equals(secondSignOffUserId)) {
                    return new EnforcementDecision(
                            EnforcementAction.BLOCK,
                            false,
                            "The second sign-off must come from a different user"
                    );
                }


                return new EnforcementDecision(
                        EnforcementAction.ALLOW_WITH_OVERRIDE,
                        true,
                        "Documented emergency, second sign-off, and reason code requirements are satisfied"
                );
            }
        }

        throw new IllegalStateException("Unsupported severity");
    }
}