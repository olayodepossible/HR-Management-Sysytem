package com.company.employee_management.repository;

import com.company.employee_management.entity.Employee;
import com.company.employee_management.enums.EmploymentStatus;
import com.company.employee_management.utils.EmployeeUtils;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class EmployeeSpecifications {
    private EmployeeSpecifications() {
    }

    public static Specification<Employee> search(String name, String department,
            EmploymentStatus status, String email) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(name)) {
                var fullName = builder.concat(builder.concat(root.get("firstName"), " "), root.get("lastName"));
                predicates.add(builder.like(builder.lower(fullName), contains(name), '\\'));
            }
            if (hasText(department)) {
                predicates.add(builder.equal(builder.lower(root.get("department").get("name")),
                        EmployeeUtils.normaliseStringToLowerCase(department)));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("employmentStatus"), status));
            }
            if (hasText(email)) {
                predicates.add(builder.like(builder.lower(root.get("email")), contains(email), '\\'));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String contains(String value) {
        String escaped = EmployeeUtils.normaliseStringToLowerCase(value)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
