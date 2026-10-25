# Drug Interaction and Allergy Checker

Java 21 Maven project for prescription safety checks.

## Requirements
- JDK 21
- Windows PowerShell

## Run tests
Run: `.\mvnw.cmd clean test`

## Features
- Drug interaction and duplicate-therapy alerts.
- Direct allergy and cross-reactivity checks.
- Configurable dose checks for age, weight, kidney function, and pregnancy.
- Brand-to-generic ingredient mapping.
- Severity-based enforcement and override validation.
- Audit records and pharmacist-authorized audit review.
- Medication activity checks before prescription finalization.

## Clinical limitations
The built-in anticoagulant-NSAID rule is an illustrative major-interaction example, not a comprehensive or clinically validated database. Additional interaction and dose rules must be configured. Clinical validation is required before real-world use.

## Project layout
- `src/main/java/` - production code.
- `src/test/java/` - automated tests.
- `pom.xml` - Maven configuration.