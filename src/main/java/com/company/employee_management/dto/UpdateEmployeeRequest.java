package com.company.employee_management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import com.company.employee_management.enums.EmploymentType;
import java.time.LocalDate;

public record UpdateEmployeeRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 100) String departmentName,
        @NotNull(message = "Employment type is required") EmploymentType employmentType,
        @NotBlank(message = "Job title is required") @Size(max = 150) String jobTitle,
        LocalDate confirmationDate,
        LocalDate probationEndDate,
        LocalDate terminationDate
) {
}
