package com.company.employee_management.service.impl;

import com.company.employee_management.dto.DepartmentResponse;
import com.company.employee_management.dto.DepartmentRequest;
import com.company.employee_management.exception.DuplicateDepartmentException;
import com.company.employee_management.exception.InvalidRequestException;
import java.util.Set;
import java.util.HashSet;
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
                .stream().map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentByName(String name) {
        String departmentName = normaliseStringToLowerCase(name);
        Department department = departmentRepository.findByNameIgnoreCase(departmentName)
                .orElseThrow(() -> new DepartmentNotFoundException(departmentName));

        return mapToResponse(department);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        return mapToResponse(findDepartment(id));
    }

    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        String name = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateDepartmentException(name);
        }
        Department department = Department.builder().name(name).description(request.description()).build();

        return mapToResponse(departmentRepository.saveAndFlush(department));
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = findDepartment(id);
        String name = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateDepartmentException(name);
        }

        department.setName(name);
        department.setDescription(request.description());
        return mapToResponse(departmentRepository.saveAndFlush(department));
    }

    private Department findDepartment(Long id) {
        return departmentRepository.findById(id).orElseThrow(() -> new DepartmentNotFoundException(id));
    }

    private DepartmentResponse mapToResponse(Department department) {
        return new DepartmentResponse(department.getId(), department.getName(), department.getDescription());
    }
}
