package com.company.employee_management.entity;

import com.company.employee_management.enums.DepartmentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Locale;
import java.util.UUID;

@Entity
@Table(
        name = "departments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_department_name",
                        columnNames = "name"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class   Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Version
    @Setter(AccessLevel.NONE)
    private Long version;

    @NotBlank(message = "Department name is required")
    @Size(max = 100)
    @Column(
            nullable = false,
            unique = true,
            length = 100
    )
    private String name;

    @NotBlank
    @Size(max = 45)
    @Column(name = "department_code", nullable = false, unique = true, updatable = false, length = 45)
    @Setter(AccessLevel.NONE)
    private String departmentCode;

    @PrePersist
    private void generateDepartmentCode() {
        departmentCode = "DEPT_REF-" + UUID.randomUUID();
    }


    @NotNull(message = "Employment status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DepartmentStatus departmentStatus = DepartmentStatus.ACTIVE;

    private String description;
}
