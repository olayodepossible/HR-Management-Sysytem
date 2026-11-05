package com.company.employee_management.dto;

import jakarta.validation.constraints.Positive;
import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateManagerRequest(@JsonProperty(value = "managerId", required = true) @Positive Long managerId) {
}
