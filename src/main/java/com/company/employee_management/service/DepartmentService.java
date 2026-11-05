package com.company.employee_management.service;

import com.company.employee_management.dto.DepartmentResponse;
import com.company.employee_management.dto.DepartmentRequest;
import java.util.List;

public interface DepartmentService {

    List<DepartmentResponse> getAllDepartments();

    DepartmentResponse getDepartmentByName(String name);

    DepartmentResponse getDepartmentById(Long id);

    DepartmentResponse createDepartment(DepartmentRequest request);

    DepartmentResponse updateDepartment(Long id, DepartmentRequest request);
}
