package com.company.employee_management.service;

import com.company.employee_management.dto.CreateEmployeeRequest;
import com.company.employee_management.dto.EmployeeResponse;
import com.company.employee_management.entity.Department;
import com.company.employee_management.entity.Employee;
import com.company.employee_management.exception.EmployeeNotFoundException;
import com.company.employee_management.repository.DepartmentRepository;
import com.company.employee_management.repository.EmployeeRepository;
import com.company.employee_management.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class  EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    private EmployeeServiceImpl employeeService;

    @BeforeEach
    void setUp() {

        employeeService = new EmployeeServiceImpl(
                employeeRepository,
                departmentRepository
        );
    }

    @Test
    void shouldCreateEmployeeUsingExistingDepartment() {

        CreateEmployeeRequest request =
                new CreateEmployeeRequest(
                        "John",
                        "Doe",
                        "john.doe@company.com",
                        "Engineering"
                );

        Department department = new Department(1L, "Engineering");

        Employee savedEmployee = Employee.builder()
                .id(10L).firstName("Doe")
                .lastName("John").email("john.doe@company.com")
                .department(department)
                .build();

        when(employeeRepository.existsByEmailIgnoreCase("john.doe@company.com")).thenReturn(false);

        when(departmentRepository.findByNameIgnoreCase("Engineering")).thenReturn(Optional.of(department));

        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);

        EmployeeResponse response = employeeService.createEmployee(request);

        assertNotNull(response);

        assertEquals(10L, response.id());

        assertEquals("Doe", response.firstName());

        assertEquals("John", response.lastName());

        assertEquals("john.doe@company.com", response.email());

        assertEquals("Engineering", response.departmentName());

        verify(employeeRepository, times(1)).save(any(Employee.class));

        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    void shouldRetrieveEmployeeById() {

        Department department = new Department(1L, "Finance");

        Employee employee = Employee.builder()
                .id(20L).firstName("Jane")
                .lastName("Smith").email("jane.smith@company.com")
                .department(department)
                .build();

        when(employeeRepository.findById(20L)).thenReturn(Optional.of(employee));

        EmployeeResponse response = employeeService.getEmployeeById(20L);

        assertNotNull(response);

        assertEquals(20L, response.id());

        assertEquals("Jane", response.firstName());

        assertEquals("Smith", response.lastName());

        assertEquals("Finance", response.departmentName());

        verify(employeeRepository, times(1)).findById(20L);
    }

    @Test
    void shouldThrowWhenEmployeeDoesNotExist() {

        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        EmployeeNotFoundException exception = assertThrows(
                EmployeeNotFoundException.class, () -> employeeService.getEmployeeById(999L));

        assertEquals("Employee with id 999 was not found", exception.getMessage());

        verify(employeeRepository, times(1)).findById(999L);
    }
}
