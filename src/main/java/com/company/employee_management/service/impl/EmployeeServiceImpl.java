package com.company.employee_management.service.impl;

import com.company.employee_management.dto.CreateEmployeeRequest;
import com.company.employee_management.dto.EmployeeResponse;
import com.company.employee_management.dto.UpdateEmployeeRequest;
import com.company.employee_management.exception.DepartmentNotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.company.employee_management.entity.Department;
import com.company.employee_management.entity.Employee;
import com.company.employee_management.exception.DuplicateEmployeeException;
import com.company.employee_management.exception.EmployeeNotFoundException;
import com.company.employee_management.repository.DepartmentRepository;
import com.company.employee_management.repository.EmployeeRepository;
import com.company.employee_management.service.EmployeeService;
import com.company.employee_management.utils.EmployeeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import static com.company.employee_management.utils.EmployeeUtils.*;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl
        implements EmployeeService {

    private final EmployeeRepository employeeRepository;

    private final DepartmentRepository departmentRepository;

    @Override
    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        String email = normaliseStringToLowerCase(request.email());

        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmployeeException(email);
        }

        String departmentName = request.departmentName().trim();

        Department department = departmentRepository
                        .findByNameIgnoreCase(departmentName)
                        .orElseGet(
                                () -> departmentRepository.save(
                                        Department.builder().name(departmentName).build()));

        Employee employee = Employee.builder()
                        .firstName(request.firstName().trim())
                        .lastName(request.lastName().trim())
                        .email(email)
                        .department(department)
                        .build();

        Employee savedEmployee = employeeRepository.save(employee);

        return mapToResponse(savedEmployee);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long employeeId) {

        Employee employee = employeeRepository.findById(employeeId)
                        .orElseThrow(() -> new EmployeeNotFoundException(employeeId));

        return mapToResponse(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getAllEmployees(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return employeeRepository.findAll(pageable).map(EmployeeUtils::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getEmployeesByDepartment(String departmentName) {
        Department department = findDepartment(departmentName);
        return employeeRepository.findByDepartment_IdOrderByIdAsc(department.getId())
                .stream().map(EmployeeUtils::mapToResponse).toList();
    }

    @Override
    @Transactional
    public EmployeeResponse updateEmployee(Long employeeId, UpdateEmployeeRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));
        String email = normaliseStringToLowerCase(request.email());
        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(email, employeeId)) {
            throw new DuplicateEmployeeException(email);
        }
        Department department = findDepartment(request.departmentName());
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setEmail(email);
        employee.setDepartment(department);
        return mapToResponse(employeeRepository.saveAndFlush(employee));
    }

    private Department findDepartment(String name) {
        String normalized = name.trim();
        return departmentRepository.findByNameIgnoreCase(normalized)
                .orElseThrow(() -> new DepartmentNotFoundException(normalized));
    }
}
