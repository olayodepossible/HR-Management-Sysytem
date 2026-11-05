package com.company.employee_management.service.impl;

import com.company.employee_management.dto.DepartmentResponse;
import com.company.employee_management.entity.Department;
import com.company.employee_management.exception.DepartmentNotFoundException;
import com.company.employee_management.repository.DepartmentRepository;
import com.company.employee_management.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

import static com.company.employee_management.utils.EmployeeUtils.normaliseStringToLowerCase;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll(Sort.by("name"))
                .stream().map(department -> new DepartmentResponse(department.getId(), department.getName()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentByName(String name) {
        String departmentName = normaliseStringToLowerCase(name);
        Department department = departmentRepository.findByNameIgnoreCase(departmentName)
                .orElseThrow(() -> new DepartmentNotFoundException(departmentName));

        return new DepartmentResponse(department.getId(), department.getName());
    }
}
