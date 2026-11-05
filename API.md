# Employee and department API

Base URL: `http://localhost:8080/api/v1`.

| Method | Path | Behavior |
| --- | --- | --- |
| POST | `/employees` | Create employee, initially PENDING |
| GET | `/employees` | Filter, paginate, and sort staff |
| GET | `/employees/{id}` | Retrieve employee |
| PUT | `/employees/{id}` | Replace editable profile fields and department assignment |
| PATCH | `/employees/{id}/status` | Change only employment status |
| PATCH | `/employees/{id}/manager` | Assign or clear reporting manager |
| GET | `/employees/departments/{name}` | Existing department staff list |
| POST | `/departments` | Create department |
| GET | `/departments` | Dropdown list, ordered by name |
| GET | `/departments/{id}` | Retrieve department by positive numeric ID |
| PUT | `/departments/{id}` | Update name and parent department |
| GET | `/departments/by-name/{name}` | Lookup department by name |

**Route change:** name lookup moved from `/departments/{name}` to
`/departments/by-name/{name}` to allow the requested ID lookup. Encode spaces
in names as `%20`.

## Filter and sort staff

```http
GET /api/v1/employees?name=Jane&department=Engineering&status=ACTIVE&email=company.com&page=0&size=20&sortBy=lastName&direction=asc
```

Filters combine with AND. `name` matches part of the full first-and-last name,
`email` matches part of the email, and `department` matches the entire department
name. String searches ignore case and surrounding whitespace; `%` and `_` are
literal search characters. Blank name, department, and email filters are ignored.
No matches, including an unknown department filter, return an empty page.

`status` accepts ACTIVE, PENDING, SUSPENDED, or DEACTIVATED, ignoring case. `suspend` is
also accepted and stored/returned as SUSPENDED. A supplied blank or invalid
status returns 400.

Pages are zero-based. Defaults: `page=0`, `size=20`, `sortBy=createdAt`,
`direction=desc`. Size must be 1–100. Allowed sort fields: `id`, `firstName`,
`lastName`, `email`, `createdAt`, `status`. Direction is `asc` or `desc`.
An ID tie-breaker keeps ordering deterministic. Invalid parameters return 400.
Responses include `content`, `number`, `size`, `totalElements`, and `totalPages`.
The existing `/employees/departments/{name}` list remains an array and returns
404 for an unknown department; use the filtered company endpoint for pagination.

## Partial status and reporting-line changes

`PATCH /api/v1/employees/1/status`:

```json
{"status":"suspend"}
```

This changes only status; names, email, department, manager, and creation time
are preserved. All three statuses may transition to any other status. Repeating
the same status is allowed.

`PATCH /api/v1/employees/1/manager`:

```json
{"managerId":2}
```

Send `{"managerId":null}` to clear the reporting line. The key is required.
The manager must exist. Self-reporting and cycles in the reporting chain are
rejected with 400. Cross-department reporting is allowed. Employee responses
include `status` and `managerId`; clients can build reporting lines from these IDs.

## Employee PUT and department assignments

`PUT /api/v1/employees/1`:

```json
{
  "firstName":"Jane",
  "lastName":"Doe",
  "email":"jane.doe@company.com",
  "departmentName":"Engineering",
  "employmentType":"FULL_TIME",
  "jobTitle":"Software Engineer"
}
```

All six fields are required. Names and department names allow at most 100
characters, and valid emails at most 150. Whitespace is trimmed and emails are
lowercased. An employee can keep their own email; another employee's email
returns 409. Status and manager are maintained through their PATCH routes.

**Update policy:** PUT requires an existing department; an unknown department
returns 404 without modifying the employee or creating a department. Create it
with POST `/departments` first. Employee creation retains the existing behavior
of creating a department if its name does not exist.

Employment details are supported by both POST and PUT employee payloads.
`employmentType` is a required enum: FULL_TIME, PART_TIME, CONTRACT, INTERN,
or TEMPORARY. `jobTitle` is required, trimmed, and limited to 150 characters.
Optional `confirmationDate`, `probationEndDate`, and `terminationDate` use
ISO dates (`YYYY-MM-DD`). PUT clears these dates when omitted or null.
Responses include the type, title, and dates. The entity field is named
`employmentStatus`, with a required enum and default PENDING; the existing
database column, response field, query filter, and PATCH payload retain `status`.
No automatic status transition is triggered by setting an employment date.
Persistent databases need the new date columns and must backfill job titles
and employment types from actual employee records before enforcing NOT NULL.

## Department management and hierarchy

POST `/departments` or PUT `/departments/1`:

```json
{"name":"Platform","parentDepartmentId":2}
```

Department names are required, trimmed, limited to 100 characters, and unique
ignoring case. POST returns 201 and a `Location` header. GET/PUT return 200.
PUT requires the full name; omitted or null `parentDepartmentId` makes the
department a root. Parents must exist, and self-parenting/ancestor cycles
are rejected with 400. Duplicate names return 409. Renaming a department
preserves employee assignments because assignments reference its ID.

All department responses contain `id`, `name`, and `parentDepartmentId`.
GET `/departments` returns these as a name-sorted array for dropdowns and for
reconstructing the department tree. Employee assignments use PUT `/employees/{id}`.

## Errors and persistence

Errors retain the `ApiError` shape: `timestamp`, `status`, `error`, `message`,
`path`, and `validationErrors`. Invalid JSON, fields, filters, and hierarchy
changes return 400; absent records/references return 404; duplicate data,
integrity conflicts, and detected concurrent record updates return 409.

The entity schema includes employee `status`, nullable `manager_id`, department
`parent_department_id`, and `version` fields for optimistic concurrency checks.
Existing databases need the parent-department column and its foreign key.
Add foreign keys
for both hierarchy references. The existing `created_at` field remains immutable.
This project currently uses Hibernate `ddl-auto: create-drop`; durable deployments
need a versioned schema migration rather than recreating existing tables.

## Verification

Employee persistence also supports `employeeCode`, `middleName`, `personalEmail`,
`phoneNumber`, `dateOfBirth`, and `profilePictureUrl`. Codes are generated at
insert as `EMP_REF-<UUID>` (44 characters), unique, and excluded from SQL updates
and public setters. Department codes use `DEPT_REF-<UUID>` (45 characters).
Optional profile fields accept null. Phone numbers must use E.164 format; birth
dates cannot be in the future; personal email and profile URL are validated.
Personal email and date of birth are excluded from direct entity JSON serialization.
These fields are not added to the existing public request/response DTOs by this
entity change. Persistent databases need nullable columns for the optional fields
and a unique, non-null `employee_code` column backfilled for existing employees.
Creation accepts optional `personalEmail`; the older misspelling `personaEmail`
is accepted as an alias. Personal email is normalized and excluded from public
responses. Existing databases must allow null in `personal_email` and widen code
columns to the lengths above.

```powershell
.\mvnw.cmd test "-Dspring.datasource.url=jdbc:h2:mem:all-tests"
```
