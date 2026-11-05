package com.company.employee_management.exception;

public class DuplicateEmployeeException extends RuntimeException {

    public DuplicateEmployeeException(String email) {
        super(
                "Employee with email " + email + " already exists"
        );
    }
}
