package com.company.employee_management.exception;

public class DepartmentNotFoundException extends RuntimeException {

    public DepartmentNotFoundException(String name) {
        super("Department with name " + name + " was not found");
    }
}
