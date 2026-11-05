package com.company.employee_management.controller;

import com.company.employee_management.dto.CreateEmployeeRequest;
import com.company.employee_management.dto.EmployeeResponse;
import com.company.employee_management.dto.UpdateEmployeeRequest;
import com.company.employee_management.dto.UpdateEmployeeStatusRequest;
import com.company.employee_management.dto.UpdateManagerRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import org.springframework.data.domain.Page;
import java.util.List;
import com.company.employee_management.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {

        EmployeeResponse response =
                employeeService.createEmployee(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable @Positive Long employeeId) {

        EmployeeResponse response =
                employeeService.getEmployeeById(employeeId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<EmployeeResponse>> getAllEmployees(
            @RequestParam(required = false) @Size(max = 201) String name,
            @RequestParam(required = false) @Size(max = 100) String department,
            @RequestParam(required = false) @Size(max = 20) String status,
            @RequestParam(required = false) @Size(max = 150) String email,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return ResponseEntity.ok(employeeService.searchEmployees(name, department, status, email,
                page, size, sortBy, direction));
    }

    @PatchMapping("/{employeeId}/status")
    public ResponseEntity<EmployeeResponse> updateStatus(
            @PathVariable @Positive Long employeeId,
            @Valid @RequestBody UpdateEmployeeStatusRequest request) {
        return ResponseEntity.ok(employeeService.updateStatus(employeeId, request.status()));
    }

    @PatchMapping("/{employeeId}/manager")
    public ResponseEntity<EmployeeResponse> updateManager(
            @PathVariable @Positive Long employeeId,
            @Valid @RequestBody UpdateManagerRequest request) {
        return ResponseEntity.ok(employeeService.updateManager(employeeId, request.managerId()));
    }

    @GetMapping("/departments/{departmentName}")
    public ResponseEntity<List<EmployeeResponse>> getEmployeesByDepartment(
            @PathVariable @NotBlank @Size(max = 100) String departmentName) {
        return ResponseEntity.ok(employeeService.getEmployeesByDepartment(departmentName));
    }

    @PutMapping("/{employeeId}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable @Positive Long employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(employeeId, request));
    }
}
