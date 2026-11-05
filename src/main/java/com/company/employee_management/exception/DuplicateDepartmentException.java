package com.company.employee_management.exception;

public class DuplicateDepartmentException extends RuntimeException {
    public DuplicateDepartmentException(String name) {
        super("Department with name " + name + " already exists");
    }
}
