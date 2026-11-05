package com.company.employee_management.repository;

import com.company.employee_management.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface EmployeeRepository  extends JpaRepository<Employee, Long> {

    @Override
    @EntityGraph(attributePaths = "department")
    Page<Employee> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "department")
    List<Employee> findByDepartment_IdOrderByIdAsc(Long departmentId);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByEmailIgnoreCase(String email);
}
