package com.company.employee_management.utils;

import com.company.employee_management.dto.EmployeeResponse;
import com.company.employee_management.entity.Employee;

import java.util.Locale;

public class EmployeeUtils {

    private EmployeeUtils(){}

    public static EmployeeResponse mapToResponse(Employee employee) {

        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getDepartment().getName()
        );
    }

    public static String normaliseStringToLowerCase(String data){
        return data.trim().toLowerCase(Locale.ROOT);
    }
}
