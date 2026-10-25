package com.example.drugchecker;

@FunctionalInterface
public interface MedicationStatusProvider {

    boolean isActive(Long medicationId);
}