package com.company.employee_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateEmployeeStatusRequest(@NotBlank @Size(max = 20) String status) {
}
