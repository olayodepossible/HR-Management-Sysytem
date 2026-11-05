package com.company.employee_management.dto;

public record EmployeeResponse(

        Long id,

        String firstName,

        String lastName,

        String email,

        String departmentName
) {
}
