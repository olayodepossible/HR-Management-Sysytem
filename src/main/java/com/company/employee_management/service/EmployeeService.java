package com.company.employee_management.service;

import com.company.employee_management.dto.CreateEmployeeRequest;
import com.company.employee_management.dto.EmployeeResponse;
import com.company.employee_management.dto.UpdateEmployeeRequest;
import java.util.List;
import org.springframework.data.domain.Page;

public interface EmployeeService {

    Page<EmployeeResponse> getAllEmployees(int page, int size);

    Page<EmployeeResponse> searchEmployees(String name, String department, String status, String email,
            int page, int size, String sortBy, String direction);

    EmployeeResponse updateStatus(Long employeeId, String status);

    EmployeeResponse updateManager(Long employeeId, Long managerId);

    List<EmployeeResponse> getEmployeesByDepartment(String departmentName);

    EmployeeResponse updateEmployee(Long employeeId,
            UpdateEmployeeRequest request);

    EmployeeResponse createEmployee(
            CreateEmployeeRequest request
    );

    EmployeeResponse getEmployeeById(
            Long employeeId
    );
}
