package com.company.employee_management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(

        @NotBlank(message = "First name is required")
        @Size(
                max = 100,
                message = "First name cannot exceed 100 characters"
        )
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(
                max = 100,
                message = "Last name cannot exceed 100 characters"
        )
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(
                max = 150,
                message = "Email cannot exceed 150 characters"
        )
        String email,

        @NotBlank(message = "Department is required")
        @Size(
                max = 100,
                message = "Department cannot exceed 100 characters"
        )
        String departmentName
) {
}
