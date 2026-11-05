package com.company.employee_management.service;

import com.company.employee_management.dto.DepartmentResponse;
import java.util.List;

public interface DepartmentService {

    List<DepartmentResponse> getAllDepartments();

    DepartmentResponse getDepartmentByName(String name);
}
