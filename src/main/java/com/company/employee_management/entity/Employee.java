package com.company.employee_management.entity;


import com.company.employee_management.enums.EmploymentStatus;
import com.company.employee_management.enums.EmploymentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Version;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.validator.constraints.URL;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "employees",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_employee_email",
                        columnNames = "email"
                )
        },
        indexes = {
                @Index(
                        name = "idx_employee_department",
                        columnList = "department_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class    Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @NotBlank
    @Size(max = 44)
    @Column(name = "employee_code", nullable = false, unique = true, updatable = false, length = 44)
    @Setter(AccessLevel.NONE)
    private String employeeCode;

    @PrePersist
    private void generateEmployeeCode() {
        employeeCode = "EMP_REF-" + UUID.randomUUID();
    }

    @Version
    @Setter(AccessLevel.NONE)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @NotNull(message = "Employment status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EmploymentStatus employmentStatus = EmploymentStatus.PENDING;

    @NotNull(message = "Employment type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 20)
    private EmploymentType employmentType;

    @NotBlank(message = "Job title is required")
    @Size(max = 150)
    @Column(name = "job_title", nullable = false, length = 150)
    private String jobTitle;

    @Column(name = "confirmation_date")
    private LocalDate confirmationDate;

    @Column(name = "probation_end_date")
    private LocalDate probationEndDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", foreignKey = @ForeignKey(name = "fk_employee_manager"))
    private Employee manager;

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    @Column(
            name = "first_name",
            nullable = false,
            length = 100
    )
    private String firstName;

    @Size(max = 100)
    @Column(name = "middle_name", length = 100)
    private String middleName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    @Column(
            name = "last_name",
            nullable = false,
            length = 100
    )
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    @Size(max = 150)
    @Column(
            nullable = false,
            unique = true,
            length = 150
    )
    private String email;

    @JsonIgnore
    @Email(message = "Enter a valid personal email")
    @Size(max = 150)
    @Column(name = "personal_email", length = 150)
    private String personalEmail;

    @Pattern(regexp = "^\\+[1-9][0-9]{1,14}$", message = "Phone number must use E.164 format, e.g. +2348012345678")
    @Size(max = 16)
    @Column(name = "phone_number", length = 16)
    private String phoneNumber;

    @JsonIgnore
    @PastOrPresent(message = "Date of birth cannot be in the future")
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @URL(message = "Profile picture URL must be valid")
    @Size(max = 2048)
    @Column(name = "profile_picture_url", length = 2048)
    private String profilePictureUrl;

    @NotNull(message = "Department is required")
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "department_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_employee_department"
            )
    )
    private Department department;
}
