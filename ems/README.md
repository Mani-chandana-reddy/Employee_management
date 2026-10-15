# Employee Management REST API (Phase 8 - Spring Boot)

A production-style REST API that lets an HR team create, view, update, search and deactivate employee records.
Data is stored in memory (`ConcurrentHashMap`) behind a repository interface, so Phase 9 can swap in JPA + PostgreSQL
without touching the service or controller layers.

**Stack:** Java 17, Spring Boot 3.3, Maven, Bean Validation, springdoc-openapi (Swagger UI), JUnit 5, Mockito, MockMvc.

## Architecture

```
Controller  ->  Service (interface + impl)  ->  Repository (interface + in-memory impl)
 HTTP only        business rules, mapping          data access only
```

- Constructor injection only (no field `@Autowired`).
- Separate DTOs (`EmployeeRequest`, `EmployeeResponse`); the `Employee` model is never exposed.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) returns one consistent error body.
- SLF4J logging: INFO for create/update/delete, DEBUG for details, WARN/ERROR in the exception handler. No emails, phones or salaries are logged.
- `app.*` properties are bound via `@ConfigurationProperties`; `app.max-salary` is enforced in the service.

```
src/main/java/com/yourname/ems/
|-- EmsApplication.java
|-- config/       AppProperties, OpenApiConfig
|-- controller/   EmployeeController
|-- dto/          EmployeeRequest, EmployeeResponse, StatusUpdateRequest, ErrorResponse, ...
|-- exception/    ResourceNotFoundException, DuplicateResourceException, InvalidRequestException, GlobalExceptionHandler
|-- filter/       RequestLoggingFilter (bonus: X-Request-Id + request logging)
|-- mapper/       EmployeeMapper
|-- model/        Employee, Department, Status
|-- repository/   EmployeeRepository, InMemoryEmployeeRepository
`-- service/      EmployeeService, EmployeeServiceImpl
```

## Run it

Prerequisites: JDK 17+ and Maven 3.9+.

```bash
mvn spring-boot:run          # start on http://localhost:8080
mvn test                     # run unit + controller tests
mvn clean package            # build target/ems-1.0.0.jar
java -jar target/ems-1.0.0.jar
```

| URL | Purpose |
|-----|---------|
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8080/v3/api-docs | OpenAPI JSON |
| http://localhost:8080/actuator/health | Health check (bonus) |

Configuration lives in `src/main/resources/application.yml` (`server.port`, logging levels, `app.name`,
`app.default-department`, `app.max-salary`).

## Endpoints

| Method | Endpoint | Description | Success / Error |
|--------|----------|-------------|-----------------|
| POST | `/api/v1/employees` | Create an employee | 201 / 400, 409 |
| GET | `/api/v1/employees` | List employees (filters below) | 200 / 400 |
| GET | `/api/v1/employees/{id}` | Get by id | 200 / 404 |
| PUT | `/api/v1/employees/{id}` | Full update | 200 / 400, 404, 409 |
| PATCH | `/api/v1/employees/{id}/status` | Activate / deactivate (soft delete) | 200 / 400, 404 |
| DELETE | `/api/v1/employees/{id}` | Permanently delete | 204 / 404 |
| GET | `/api/v1/employees/search?keyword=` | Search first/last name or email | 200 / 400 |
| GET | `/api/v1/employees/department/{dept}` | Employees of a department | 200 / 400 |
| GET | `/api/v1/employees/stats` | Headcount per department + average salary | 200 |

List filters: `?department=IT&status=ACTIVE&sortBy=salary&order=desc`
(`sortBy`: id, firstName, lastName, email, department, designation, salary, dateOfJoining, status; `order`: asc/desc).

### Validation rules

| Field | Rule |
|-------|------|
| firstName, lastName | required, 2-50 chars, letters only |
| email | required, valid format, unique (case-insensitive) |
| phone | required, exactly 10 digits |
| department | required: HR, IT, FINANCE, SALES, OPERATIONS |
| designation | required, max 60 chars |
| salary | required, > 0 and <= `app.max-salary` |
| dateOfJoining | required, not in the future |

## Sample requests and responses

**Create** - `POST /api/v1/employees`

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phone": "9876543210",
  "department": "IT",
  "designation": "Software Engineer",
  "salary": 75000.00,
  "dateOfJoining": "2024-01-15"
}
```

`201 Created` (with `Location: /api/v1/employees/1`)

```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phone": "9876543210",
  "department": "IT",
  "designation": "Software Engineer",
  "salary": 75000.00,
  "dateOfJoining": "2024-01-15",
  "status": "ACTIVE"
}
```

**Deactivate** - `PATCH /api/v1/employees/1/status` with `{ "status": "INACTIVE" }` -> `200` with the updated employee.

**Stats** - `GET /api/v1/employees/stats`

```json
{
  "totalEmployees": 2,
  "departments": [
    { "department": "HR", "headcount": 1, "averageSalary": 82000.50 },
    { "department": "IT", "headcount": 1, "averageSalary": 75000.00 },
    { "department": "FINANCE", "headcount": 0, "averageSalary": 0.00 },
    { "department": "SALES", "headcount": 0, "averageSalary": 0.00 },
    { "department": "OPERATIONS", "headcount": 0, "averageSalary": 0.00 }
  ]
}
```

**Not found** - `GET /api/v1/employees/15` -> `404`

```json
{
  "timestamp": "2026-10-15T10:15:30",
  "status": 404,
  "error": "Not Found",
  "message": "Employee not found with id 15",
  "path": "/api/v1/employees/15"
}
```

**Validation failure** - `400`

```json
{
  "timestamp": "2026-10-15T10:16:02",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/employees",
  "fieldErrors": [
    { "field": "firstName", "message": "First name must be 2-50 characters" },
    { "field": "email", "message": "Email must be a valid email address" },
    { "field": "phone", "message": "Phone must be a 10-digit number" }
  ]
}
```

**Duplicate email** - `409 Conflict`, `"message": "An employee with the given email already exists"`.

## Tests

- `EmployeeServiceTest` (JUnit 5 + Mockito): create, duplicate email, salary above max, get not found, update,
  update with another employee's email, deactivate, search, filter + sort, delete not found, stats.
- `EmployeeControllerTest` (`@WebMvcTest` + MockMvc): 201 on create, 400 on invalid input, 404 on missing id,
  409 duplicate, filters, PATCH, DELETE, `X-Request-Id` header.
- `EmsApplicationTests`: context loads.

## API collections

- `docs/employee-api.http` - IntelliJ HTTP Client / VS Code REST Client, all endpoints + 15 failure scenarios.
- `docs/Employee-API.postman_collection.json` - import into Postman (uses `{{baseUrl}}`, includes status-code assertions).
  Run the collection top to bottom against a fresh server so ids and duplicates line up.
- Add your Swagger UI screenshots to `docs/screenshots/`.

## Bonus features included

- `X-Request-Id` response header + request logging (method, URL, status, time) via `RequestLoggingFilter`
- `/actuator/health` via Spring Boot Actuator

## Git workflow

```bash
git init && git checkout -b main
git add . && git commit -m "chore: initial Spring Boot project"
git checkout -b feature/employee-crud     # work on feature branches, open PRs into main
```
