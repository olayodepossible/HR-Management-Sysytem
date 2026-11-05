package com.company.employee_management.controller;

import com.company.employee_management.entity.Department;
import com.company.employee_management.entity.Employee;
import com.company.employee_management.repository.DepartmentRepository;
import com.company.employee_management.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:staff-api-tests")
@Transactional
class EmployeeControllerApiTest {
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired WebApplicationContext context;
    @Autowired EmployeeRepository employees;
    @Autowired DepartmentRepository departments;
    MockMvc mvc;
    Employee employee;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        employees.deleteAll();
        departments.deleteAll();
        Department engineering = departments.save(new Department(null, "Engineering"));
        departments.save(new Department(null, "Finance"));
        employee = employees.saveAndFlush(Employee.builder().firstName("Jane").lastName("Doe")
                .email("jane@company.com").department(engineering).build());
    }

    @Test
    void listsCompanyStaffAndFiltersCaseInsensitively() throws Exception {
        mvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/employees/departments/engineering"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].email").value("jane@company.com"));
        mvc.perform(get("/api/v1/employees/departments/Finance"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/v1/employees/departments/Unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listsDepartmentsForDropdownAndLooksUpByPath() throws Exception {
        mvc.perform(get("/api/v1/departments"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Engineering"));
        mvc.perform(get("/api/v1/departments/finance"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Finance"));
        mvc.perform(get("/api/v1/departments/Unknown")).andExpect(status().isNotFound());
    }

    @Test
    void updatesStaffAndAllowsKeepingOwnEmail() throws Exception {
        mvc.perform(put("/api/v1/employees/" + employee.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(payload("jane@company.com", "finance")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.firstName").value("Janet"))
                .andExpect(jsonPath("$.departmentName").value("Finance"));
        assertEquals("Finance", employees.findById(employee.getId()).orElseThrow().getDepartment().getName());
    }

    @Test
    void rejectsDuplicateEmailAndUnknownRecords() throws Exception {
        employees.saveAndFlush(Employee.builder().firstName("John").lastName("Smith")
                .email("john@company.com").department(employee.getDepartment()).build());
        mvc.perform(put("/api/v1/employees/" + employee.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(payload("JOHN@company.com", "Engineering")))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/v1/employees/999999").contentType(MediaType.APPLICATION_JSON)
                        .content(payload("jane@company.com", "Engineering")))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/employees/" + employee.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(payload("jane@company.com", "Unknown")))
                .andExpect(status().isNotFound());
        assertEquals("Jane", employee.getFirstName());
    }

    @Test
    void rejectsInvalidPayloadsAndPathValues() throws Exception {
        mvc.perform(put("/api/v1/employees/" + employee.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(payload("invalid", "Engineering")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.validationErrors.email").exists());
        mvc.perform(put("/api/v1/employees/" + employee.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/employees/" + employee.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/employees/-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/departments/{departmentName}", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsEmptyCompanyListAndPreservesIdLookup() throws Exception {
        mvc.perform(get("/api/v1/employees/" + employee.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("jane@company.com"));
        employees.deleteAll();
        mvc.perform(get("/api/v1/employees")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private String payload(String email, String department) {
        return """
                {"firstName":" Janet ","lastName":"Doe","email":"%s","departmentName":"%s"}
                """.formatted(email, department);
    }

    @Test
    void paginatesByCreationDateDescendingRatherThanId() throws Exception {
        Employee older = employees.saveAndFlush(Employee.builder().firstName("Older").lastName("Staff")
                .email("older@company.com").department(employee.getDepartment()).build());
        org.junit.jupiter.api.Assertions.assertNotNull(older.getCreatedAt());
        entityManager.createNativeQuery("update employees set created_at = :created where id = :id")
                .setParameter("created", java.time.Instant.parse("2020-01-01T00:00:00Z"))
                .setParameter("id", older.getId()).executeUpdate();
        entityManager.clear();

        mvc.perform(get("/api/v1/employees").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("jane@company.com"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0));
        mvc.perform(get("/api/v1/employees").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("older@company.com"));
        mvc.perform(get("/api/v1/employees").param("page", "2").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void rejectsInvalidPagination() throws Exception {
        mvc.perform(get("/api/v1/employees").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/employees").param("size", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/employees").param("size", "101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/employees").param("page", "abc")).andExpect(status().isBadRequest());
    }
}
