package com.company.employee_management;

import com.company.employee_management.entity.Department;
import com.company.employee_management.entity.Employee;
import com.company.employee_management.enums.EmploymentType;
import com.company.employee_management.repository.DepartmentRepository;
import com.company.employee_management.repository.EmployeeRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:identifier-tests")
@Transactional
class IdentifierPersistenceTest {
    @Autowired DepartmentRepository departments;
    @Autowired EmployeeRepository employees;
    @Autowired EntityManager entityManager;
    @Autowired Validator validator;

    @Test
    void generatedCodesFitValidationAndDatabaseColumns() {
        Department department = departments.saveAndFlush(Department.builder().name("Identifier Test").build());
        String departmentCode = department.getDepartmentCode();
        assertEquals(45, departmentCode.length());
        assertTrue(departmentCode.startsWith("DEPT_REF-"));
        assertTrue(validator.validate(department).isEmpty());

        Employee employee = employees.saveAndFlush(Employee.builder().firstName("Code").lastName("Test")
                .email("code@company.com").personalEmail("code@example.com")
                .employmentType(EmploymentType.FULL_TIME).jobTitle("Engineer").department(department).build());
        String employeeCode = employee.getEmployeeCode();
        assertEquals(44, employeeCode.length());
        assertTrue(employeeCode.startsWith("EMP_REF-"));
        assertTrue(validator.validate(employee).isEmpty());

        Long employeeId = employee.getId();
        Long departmentId = department.getId();
        entityManager.clear();
        assertEquals(employeeCode, employees.findById(employeeId).orElseThrow().getEmployeeCode());
        assertEquals(departmentCode, departments.findById(departmentId).orElseThrow().getDepartmentCode());
    }
}
