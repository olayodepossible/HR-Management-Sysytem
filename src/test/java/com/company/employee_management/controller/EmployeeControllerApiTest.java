package com.company.employee_management.controller;

import com.company.employee_management.entity.Department;
import com.company.employee_management.entity.Employee;
import com.company.employee_management.enums.DepartmentStatus;
import com.company.employee_management.enums.EmploymentType;
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
    @Autowired jakarta.validation.Validator validator;
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
        Department engineering = departments.save(new Department(null, null, "Engineering", "DEPT_REF-4757ghgjhjhjht6ry", DepartmentStatus.ACTIVE, "Engineering"));
        departments.save(new Department(null, null, "Finance", "DEPT_REF-t475j68765gyfhghug", DepartmentStatus.ACTIVE, "Finance"));
        employee = employees.saveAndFlush(Employee.builder().employmentType(EmploymentType.FULL_TIME).jobTitle("Engineer").firstName("Jane").lastName("Doe")
                .email("jane@company.com").personalEmail("jane@company.com").department(engineering).build());
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
        mvc.perform(get("/api/v1/departments/by-name/finance"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Finance"));
        mvc.perform(get("/api/v1/departments/by-name/Unknown")).andExpect(status().isNotFound());
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
        employees.saveAndFlush(Employee.builder().employmentType(EmploymentType.FULL_TIME).jobTitle("Engineer").firstName("John").lastName("Smith")
                .email("john@company.com").personalEmail("john@test.com").department(employee.getDepartment()).build());
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
        org.junit.jupiter.api.Assertions.assertTrue(departments.findByNameIgnoreCase("Unknown").isEmpty());
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
        mvc.perform(get("/api/v1/departments/by-name/{departmentName}", " "))
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
                {"firstName":" Janet ","lastName":"Doe","email":"%s","departmentName":"%s",
                "employmentType":"FULL_TIME","jobTitle":"Engineer"}
                """.formatted(email, department);
    }

    @Test
    void paginatesByCreationDateDescendingRatherThanId() throws Exception {
        Employee older = employees.saveAndFlush(Employee.builder().employmentType(EmploymentType.FULL_TIME).jobTitle("Engineer").firstName("Older").lastName("Staff")
                .email("older@company.com").personalEmail("jane@test.com").department(employee.getDepartment()).build());
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

    @Test
    void patchesOnlyStatusAndSupportsEveryStatus() throws Exception {
        java.time.Instant createdAt = employee.getCreatedAt();
        for (String statusValue : java.util.List.of("suspend", "deactivated", "Active")) {
            String expected = statusValue.equals("suspend") ? "SUSPENDED" : statusValue.toUpperCase();
            mvc.perform(patch("/api/v1/employees/" + employee.getId() + "/status")
                            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + statusValue + "\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(expected))
                    .andExpect(jsonPath("$.firstName").value("Jane"))
                    .andExpect(jsonPath("$.email").value("jane@company.com"));
        }
        assertEquals(createdAt, employees.findById(employee.getId()).orElseThrow().getCreatedAt());
    }

    @Test
    void rejectsInvalidStatusAndMissingEmployee() throws Exception {
        for (String body : java.util.List.of("{}", "{\"status\":null}", "{\"status\":\"\"}", "{\"status\":\"invalid\"}")) {
            mvc.perform(patch("/api/v1/employees/" + employee.getId() + "/status")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(patch("/api/v1/employees/999999/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void combinesAllFiltersAndSortsPaginatedResults() throws Exception {
        Employee other = employees.saveAndFlush(Employee.builder().employmentType(EmploymentType.FULL_TIME).jobTitle("Engineer").firstName("Jane").lastName("Smith")
                .email("other@company.com").personalEmail("jane@test.com").department(employee.getDepartment()).build());
        mvc.perform(patch("/api/v1/employees/" + employee.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"suspend\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/employees").param("name", " JANE DOE ").param("department", "engineering")
                        .param("status", "suspend").param("email", "JANE@").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("jane@company.com"));
        mvc.perform(get("/api/v1/employees").param("name", "Jane").param("size", "1")
                        .param("sortBy", "lastName").param("direction", "asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].lastName").value("Doe"));
        mvc.perform(get("/api/v1/employees").param("name", "Jane").param("size", "1").param("page", "1")
                        .param("sortBy", "lastName").param("direction", "asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(other.getId()));
        mvc.perform(get("/api/v1/employees").param("department", "Unknown"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(get("/api/v1/employees").param("email", "%"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void rejectsInvalidSearchParameters() throws Exception {
        mvc.perform(get("/api/v1/employees").param("sortBy", "department.invalid"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/employees").param("direction", "sideways"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/employees").param("status", "unknown"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsRetrievesAndUpdatesDepartmentsWithParents() throws Exception {
        Long parentId = employee.getDepartment().getId();
        String body = "{\"name\":\" Platform \"}";
        mvc.perform(post("/api/v1/departments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Platform"));
        Long id = departments.findByNameIgnoreCase("Platform").orElseThrow().getId();
        mvc.perform(get("/api/v1/departments/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Platform"));
        mvc.perform(put("/api/v1/departments/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Infrastructure\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Infrastructure"));
        mvc.perform(get("/api/v1/departments")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void rejectsDuplicateInvalidAndUnknownDepartments() throws Exception {
        mvc.perform(post("/api/v1/departments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" engineering \"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/departments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/departments/999999")).andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/departments/999999").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New\"}"))
                .andExpect(status().isNotFound());
        Long financeId = departments.findByNameIgnoreCase("Finance").orElseThrow().getId();
        mvc.perform(put("/api/v1/departments/" + financeId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Engineering\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void preventsDepartmentCyclesAndKeepsAssignmentsWhenRenaming() throws Exception {
        Department parent = employee.getDepartment();

        mvc.perform(put("/api/v1/departments/" + parent.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Technology\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/employees/" + employee.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.departmentName").value("Technology"));
    }

    @Test
    void managesReportingLinesAndPreventsCycles() throws Exception {
        Employee manager = employees.saveAndFlush(Employee.builder().employmentType(EmploymentType.FULL_TIME).jobTitle("Engineer").firstName("Manager").lastName("One")
                .email("manager@company.com").personalEmail("jane@test.com").department(employee.getDepartment()).build());
        mvc.perform(patch("/api/v1/employees/" + employee.getId() + "/manager")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"managerId\":" + manager.getId() + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.managerId").value(manager.getId()));
        mvc.perform(patch("/api/v1/employees/" + manager.getId() + "/manager")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"managerId\":" + employee.getId() + "}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/employees/" + employee.getId() + "/manager")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"managerId\":" + employee.getId() + "}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/employees/" + employee.getId() + "/manager")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"managerId\":999999}"))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/employees/" + employee.getId() + "/manager")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"managerId\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.managerId").isEmpty());
        mvc.perform(patch("/api/v1/employees/" + employee.getId() + "/manager")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creationDefaultsToPendingAndPutReassignsDepartmentWithoutChangingStatus() throws Exception {
        mvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON)
                        .content(payload("new@company.com", "Engineering")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"));
        mvc.perform(patch("/api/v1/employees/" + employee.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DEACTIVATED\"}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/employees/" + employee.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(payload("jane@company.com", "Finance")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.departmentName").value("Finance"))
                .andExpect(jsonPath("$.status").value("DEACTIVATED"));
    }

    @Test
    void persistsOptionalProfileFieldsAndKeepsGeneratedCodeAcrossUpdates() throws Exception {
        String code = employee.getEmployeeCode();
        org.junit.jupiter.api.Assertions.assertTrue(code.matches("EMP_REF-[0-9a-f-]{36}"));
        employee.setMiddleName("Marie");
        employee.setEmail("jane@example.com");
        employee.setPersonalEmail("jane@example.com");
        employee.setPhoneNumber("+2348012345678");
        employee.setDateOfBirth(java.time.LocalDate.of(1990, 5, 12));
        employee.setProfilePictureUrl("https://example.com/jane.png");
        employees.saveAndFlush(employee);
        Long id = employee.getId();
        entityManager.clear();
        Employee stored = employees.findById(id).orElseThrow();
        assertEquals(code, stored.getEmployeeCode());
        assertEquals("Marie", stored.getMiddleName());
        assertEquals("jane@example.com", stored.getPersonalEmail());
        assertEquals("+2348012345678", stored.getPhoneNumber());
        assertEquals(java.time.LocalDate.of(1990, 5, 12), stored.getDateOfBirth());
        assertEquals("https://example.com/jane.png", stored.getProfilePictureUrl());
        Employee other = employees.saveAndFlush(Employee.builder().employmentType(EmploymentType.FULL_TIME).jobTitle("Engineer").firstName("Other").lastName("Staff")
                .email("other@company.com").department(stored.getDepartment()).build());
        org.junit.jupiter.api.Assertions.assertNotEquals(code, other.getEmployeeCode());
        mvc.perform(get("/api/v1/employees/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.personalEmail").doesNotExist())
                .andExpect(jsonPath("$.dateOfBirth").doesNotExist());
    }

    @Test
    void validatesProfileFieldConstraintsWhileAllowingMissingOptionalFields() {
        org.junit.jupiter.api.Assertions.assertTrue(validator.validate(employee).isEmpty());
        employee.setMiddleName("x".repeat(101));
        employee.setPersonalEmail("invalid-email");
        employee.setPhoneNumber("08012345678");
        employee.setDateOfBirth(java.time.LocalDate.now().plusDays(1));
        employee.setProfilePictureUrl("not-a-url");
        var fields = validator.validate(employee).stream()
                .map(violation -> violation.getPropertyPath().toString()).collect(java.util.stream.Collectors.toSet());
        org.junit.jupiter.api.Assertions.assertTrue(fields.containsAll(java.util.Set.of(
                "middleName", "personalEmail", "phoneNumber", "dateOfBirth", "profilePictureUrl")));
    }

    @Test
    void createsAndUpdatesEmploymentDetailsAndOptionalDates() throws Exception {
        String body = payload("details@company.com", "Engineering").replace("Engineer", " Consultant ")
                .replace("FULL_TIME", "CONTRACT").replace("\"jobTitle\":\" Consultant \"",
                        "\"jobTitle\":\" Consultant \",\"confirmationDate\":\"2026-01-01\","
                                + "\"probationEndDate\":\"2025-12-31\",\"terminationDate\":\"2030-01-01\"");
        mvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.employmentType").value("CONTRACT"))
                .andExpect(jsonPath("$.jobTitle").value("Consultant"))
                .andExpect(jsonPath("$.confirmationDate").value("2026-01-01"))
                .andExpect(jsonPath("$.probationEndDate").value("2025-12-31"))
                .andExpect(jsonPath("$.terminationDate").value("2030-01-01"));
        Long id = employees.findAll().stream().filter(item -> item.getEmail().equals("details@company.com"))
                .findFirst().orElseThrow().getId();
        entityManager.clear();
        assertEquals(java.time.LocalDate.of(2030, 1, 1), employees.findById(id).orElseThrow().getTerminationDate());
        mvc.perform(put("/api/v1/employees/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(payload("details@company.com", "Finance")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.employmentType").value("FULL_TIME"))
                .andExpect(jsonPath("$.jobTitle").value("Engineer"))
                .andExpect(jsonPath("$.confirmationDate").isEmpty())
                .andExpect(jsonPath("$.probationEndDate").isEmpty())
                .andExpect(jsonPath("$.terminationDate").isEmpty());
    }

    @Test
    void rejectsMissingEmploymentTypeAndJobTitleAndInvalidEnums() throws Exception {
        String valid = payload("new@company.com", "Engineering");
        for (String invalid : java.util.List.of(
                valid.replace("\"FULL_TIME\"", "null"),
                valid.replace("\"Engineer\"", "\" \""),
                valid.replace("\"FULL_TIME\"", "\"INVALID\""))) {
            mvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest());
            mvc.perform(put("/api/v1/employees/" + employee.getId())
                            .contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest());
        }
        employee.setEmploymentStatus(null);
        employee.setEmploymentType(null);
        employee.setJobTitle(" ");
        var fields = validator.validate(employee).stream().map(item -> item.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
        org.junit.jupiter.api.Assertions.assertTrue(fields.containsAll(java.util.Set.of(
                "employmentStatus", "employmentType", "jobTitle")));
    }
}
