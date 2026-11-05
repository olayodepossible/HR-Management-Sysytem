package com.company.employee_management.enums;

import java.util.Locale;
import com.company.employee_management.exception.InvalidRequestException;

public enum EmploymentStatus {
    ACTIVE, PENDING, SUSPENDED, DEACTIVATED;

    public static EmploymentStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException("Status is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("SUSPEND")) {
            return SUSPENDED;
        }
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Status must be ACTIVE, SUSPENDED (or suspend), or DEACTIVATED");
        }
    }
}
