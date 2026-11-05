package com.company.employee_management.dto;

import com.company.employee_management.enums.EmploymentStatus;
import com.company.employee_management.enums.EmploymentType;
import java.time.LocalDate;

public record EmployeeResponse(

        Long id,

        String firstName,

        String lastName,

        String email,

        String departmentName,

        EmploymentStatus status,

        Long managerId,
        EmploymentType employmentType,
        String jobTitle,
        LocalDate confirmationDate,
        LocalDate probationEndDate,
        LocalDate terminationDate
) {
}
