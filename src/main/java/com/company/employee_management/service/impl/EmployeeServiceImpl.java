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
import com.company.employee_management.enums.EmploymentStatus;
import com.company.employee_management.exception.InvalidRequestException;
import com.company.employee_management.repository.EmployeeSpecifications;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;
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
                        .personalEmail(request.personalEmail() == null || request.personalEmail().isBlank()
                                ? null : normaliseStringToLowerCase(request.personalEmail()))
                        .department(department)
                        .employmentType(request.employmentType())
                        .jobTitle(request.jobTitle().trim())
                        .confirmationDate(request.confirmationDate())
                        .probationEndDate(request.probationEndDate())
                        .terminationDate(request.terminationDate())
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
        return searchEmployees(null, null, null, null, page, size, "createdAt", "desc");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> searchEmployees(String name, String department, String status, String email,
            int page, int size, String sortBy, String direction) {
        Set<String> allowedSorts = Set.of("id", "firstName", "lastName", "email", "createdAt", "status");
        if (page < 0 || size < 1 || size > 100 || sortBy == null || !allowedSorts.contains(sortBy)
                || direction == null || !(direction.equalsIgnoreCase("asc") || direction.equalsIgnoreCase("desc"))) {
            throw new InvalidRequestException("Invalid pagination or sorting parameters");
        }
        EmploymentStatus employmentStatus = status == null ? null : EmploymentStatus.parse(status);
        Sort.Direction sortDirection = Sort.Direction.valueOf(direction.toUpperCase(Locale.ROOT));
        String property = sortBy.equals("status") ? "employmentStatus" : sortBy;
        Sort sort = Sort.by(sortDirection, property);
        if (!sortBy.equals("id")) {
            sort = sort.and(Sort.by(sortDirection, "id"));
        }
        return employeeRepository.findAll(EmployeeSpecifications.search(name, department, employmentStatus, email),
                PageRequest.of(page, size, sort)).map(EmployeeUtils::mapToResponse);
    }

    @Override
    @Transactional
    public EmployeeResponse updateStatus(Long employeeId, String status) {
        EmploymentStatus requestedStatus = EmploymentStatus.parse(status);
        Employee employee = findEmployee(employeeId);
        employee.setEmploymentStatus(requestedStatus);
        return mapToResponse(employeeRepository.saveAndFlush(employee));
    }

    @Override
    @Transactional
    public EmployeeResponse updateManager(Long employeeId, Long managerId) {
        Employee employee = findEmployee(employeeId);
        Employee manager = managerId == null ? null : findEmployee(managerId);
        Set<Long> visited = new HashSet<>();
        for (Employee current = manager; current != null; current = current.getManager()) {
            if (current.getId().equals(employeeId) || !visited.add(current.getId())) {
                throw new InvalidRequestException("Reporting lines cannot contain a cycle");
            }
        }
        employee.setManager(manager);
        return mapToResponse(employeeRepository.saveAndFlush(employee));
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
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
        employee.setEmploymentType(request.employmentType());
        employee.setJobTitle(request.jobTitle().trim());
        employee.setConfirmationDate(request.confirmationDate());
        employee.setProbationEndDate(request.probationEndDate());
        employee.setTerminationDate(request.terminationDate());
        return mapToResponse(employeeRepository.saveAndFlush(employee));
    }

    private Department findDepartment(String name) {
        String normalized = name.trim();
        return departmentRepository.findByNameIgnoreCase(normalized)
                .orElseThrow(() -> new DepartmentNotFoundException(normalized));
    }
}
