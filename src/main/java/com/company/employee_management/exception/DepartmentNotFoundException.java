package com.company.employee_management.exception;

public class DepartmentNotFoundException extends RuntimeException {

    public DepartmentNotFoundException(Long id) {
        super("Department with id " + id + " was not found");
    }

    public DepartmentNotFoundException(String name) {
        super("Department with name " + name + " was not found");
    }
}
